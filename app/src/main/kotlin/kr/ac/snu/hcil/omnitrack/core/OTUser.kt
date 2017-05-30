package kr.ac.snu.hcil.omnitrack.core

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.*
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.DatabaseManager
import kr.ac.snu.hcil.omnitrack.core.externals.fitbit.FitbitRecentSleepTimeMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTimeTrigger
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerManager
import kr.ac.snu.hcil.omnitrack.utils.*
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetUpdateService
import rx.Observable
import rx.Single
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import rx.subscriptions.CompositeSubscription
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */
class OTUser(val objectId: String, var name: String?, var photoUrl: String?, _trackers: List<OTTracker>? = null) {

    companion object {

        const val PREFERENCES_KEY_OBJECT_ID = "ot_user_object_id"
        const val PREFERENCES_KEY_NAME = "ot_user_name"
        const val PREFERENCES_KEY_PHOTO_URL = "ot_user_photo_url"


        //const val PREFERENCES_KEY_EMAIL = "user_email"

        fun isUserStored(sp: SharedPreferences): Boolean {
            return sp.contains(PREFERENCES_KEY_OBJECT_ID)
        }

        fun loadCachedInstance(sp: SharedPreferences): OTUser? {
            if (isUserStored(sp)) {
                val objId = sp.getString(PREFERENCES_KEY_OBJECT_ID, null)
                val name = sp.getString(PREFERENCES_KEY_NAME, null)
                val photoUrl = sp.getString(PREFERENCES_KEY_PHOTO_URL, null)
                if (objId != null && name != null) {
                    /*
                    return DatabaseManager.findTrackersOfUser(objId).flatMap {
                        trackers ->
                        val user = OTUser(objId, name, photoUrl, trackers)
                        DatabaseManager.findTriggersOfUser(user).map {
                            triggers ->
                            for (trigger in triggers) {
                                user.triggerManager.putNewTrigger(trigger)
                            }
                            user
                        }
                    }*/
                    return OTUser(objId, name, photoUrl)
                } else return null
            } else return null
        }

        fun storeOrOverwriteInstanceCache(instance: OTUser, sp: SharedPreferences): Boolean {
            val overwritten = sp.contains(PREFERENCES_KEY_OBJECT_ID)

            sp.edit()
                    .putString(PREFERENCES_KEY_OBJECT_ID, instance.objectId)
                    .putString(PREFERENCES_KEY_NAME, instance.name)
                    .putString(PREFERENCES_KEY_PHOTO_URL, instance.photoUrl)
                    .apply()

            return overwritten
        }

        fun clearInstanceCache(sp: SharedPreferences): Boolean {
            val removed = sp.contains(PREFERENCES_KEY_OBJECT_ID)

            sp.edit()
                    .remove(PREFERENCES_KEY_OBJECT_ID)
                    .remove(PREFERENCES_KEY_NAME)
                    .remove(PREFERENCES_KEY_PHOTO_URL)
                    .apply()

            return removed
        }
    }

    /*
    val email: String by Delegates.observable(email) {
        prop, old, new ->
        if (old != new) {
            isDirtySinceLastSync = true
        }
    }*/

    private val databaseRef: DatabaseReference?
        get() = DatabaseManager.dbRef?.child(DatabaseManager.CHILD_NAME_USERS)?.child(objectId)

    val trackers = ObservableList<OTTracker>()

    val triggerManager: OTTriggerManager

    //val trackerAdded = Event<ReadOnlyPair<OTTracker, Int>>()

    private val subscriptions = CompositeSubscription()

    val trackerAdded = SerializedSubject(PublishSubject.create<ReadOnlyPair<OTTracker, Int>>())
    val trackerRemoved = SerializedSubject(PublishSubject.create<ReadOnlyPair<OTTracker, Int>>())
    val trackerIndexChanged = SerializedSubject(PublishSubject.create<ReadOnlyPair<OTTracker, Int>>())

    private var trackerListDbReference: DatabaseReference? = null
    private val trackerListChangeEventListener: ChildEventListener

    private var triggerListDbReference: DatabaseReference? = null
    private val triggerListChangeEventListener: ChildEventListener

    var suspendDatabaseSync: Boolean = false

    init {

        if (_trackers != null) {
            for (tracker: OTTracker in _trackers) {
                trackers.unObservedList.add(tracker)

                tracker.addedToUser.suspend = true
                tracker.owner = this
                tracker.addedToUser.suspend = false

            }
        }

        triggerManager = OTTriggerManager(this)

        trackers.elementAdded += { sender, args ->
            onTrackerAdded(args.first, args.second)
        }

        trackers.elementRemoved += { sender, args ->
            onTrackerRemoved(args.first, args.second)
        }

        trackers.elementReordered += {
            sender, range ->
            for (i in range) {
                trackers[i].databasePointRef?.child("position")?.setValue(i)
            }
        }

        trackers.listModified += {
            sender, args ->
            OTApplication.app.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(OTApplication.app))
        }

        trackerListDbReference = databaseRef?.child(DatabaseManager.CHILD_NAME_TRACKERS)
        triggerListDbReference = databaseRef?.child(DatabaseManager.CHILD_NAME_TRIGGERS)


        trackerListChangeEventListener = object : ChildEventListener {
            override fun onCancelled(error: DatabaseError) {
                println("trackers error: ${error.toException().printStackTrace()}")
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildKey: String?) {
                println("tracker child added : ${snapshot.key}")
                val duplicate = trackers.unObservedList.find { it.objectId == snapshot.key }
                if (duplicate == null) {
                    println("load tracker ${snapshot.key} from DB")
                    DatabaseManager.getTracker(snapshot.key).subscribe {
                        tracker ->

                        suspendDatabaseSync = true
                        tracker.suspendDatabaseSync = true
                        tracker.owner = this@OTUser
                        this@OTUser.trackers += tracker
                        tracker.suspendDatabaseSync = false
                        suspendDatabaseSync = false
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildKey: String?) {
                println("tracker child changed : ${snapshot.key}")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildKey: String?) {

                println("tracker child moved : ${snapshot.key}")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                println("tracker child removed: ${snapshot.key}")
                val duplicate = trackers.unObservedList.find { it.objectId == snapshot.key }
                if (duplicate != null) {
                    suspendDatabaseSync = true
                    duplicate.dispose()
                    trackers.remove(duplicate)
                    suspendDatabaseSync = false
                }
            }

        }

        triggerListChangeEventListener = object : ChildEventListener {
            override fun onCancelled(error: DatabaseError) {
                println("triggers error: ${error.toException().printStackTrace()}")
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildKey: String?) {
                println("trigger child added : ${snapshot.key}")
                val duplicate = triggerManager.getTriggerWithId(snapshot.key)
                if (duplicate == null) {
                    println("load trigger ${snapshot.key} from DB")
                    DatabaseManager.getTrigger(this@OTUser, snapshot.key).subscribe({
                        trigger ->
                        triggerManager.putNewTrigger(trigger)
                    }, {})
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildKey: String?) {
                println("trigger child changed : ${snapshot.key}")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildKey: String?) {

                println("trigger child moved : ${snapshot.key}")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                println("trigger child removed: ${snapshot.key}")
                val duplicate = triggerManager.getTriggerWithId(snapshot.key)
                if (duplicate != null) {
                    triggerManager.removeTrigger(duplicate)
                }
            }

        }

        trackerListDbReference?.addChildEventListener(trackerListChangeEventListener)
        triggerListDbReference?.addChildEventListener(triggerListChangeEventListener)


    }

    fun getTrackersOnShortcut(): List<OTTracker>{
        return trackers.filter { it.isOnShortcut == true }
    }

    /*
    fun getRecentTrackers(): List<OTTracker>{
        val list = trackers.unObservedList.toMutableList()
        list.map { OTApplication.app.dbHelper.getLastLoggingTimeAsync(it,) }
    }*/

    fun detachFromSystem() {
        triggerManager.detachFromSystem()
        trackerListDbReference?.removeEventListener(trackerListChangeEventListener)
        triggerListDbReference?.removeEventListener(triggerListChangeEventListener)
        subscriptions.clear()
    }

    fun newTrackerWithDefaultName(context: Context, add: Boolean): OTTracker {
        return newTracker(this.generateNewTrackerName(context), add, isEditable = true)
    }

    fun newTracker(name: String, add: Boolean, creationFlags: Map<String, String>? = null, isEditable: Boolean = true): OTTracker {
        val tracker = OTTracker(null, name, isOnShortcut = false, isEditable = isEditable, creationFlags = creationFlags)
        val unOccupied = OTApplication.app.colorPalette.filter {
            color ->
            trackers.unObservedList.find {
                tracker ->
                tracker.color == color
            } == null
        }

        tracker.color = if (unOccupied.isNotEmpty()) {
            unOccupied.first()
        } else {
            OTApplication.app.colorPalette.first()
        }

        if (add) {
            trackers.add(tracker)
        }

        return tracker
    }

    fun getPermissionsRequiredForFields(): Set<String> {
        val set = HashSet<String>()
        for (tracker in trackers) {
            for (attr in tracker.attributes) {
                val permissions = attr.requiredPermissions()
                if (permissions != null) {
                    set.addAll(permissions)
                }
            }
        }

        return set
    }

    private fun onTrackerAdded(new: OTTracker, index: Int) {
        new.owner = this
        if (new.isOnShortcut) {
            OTShortcutPanelManager += new
        }

        trackerAdded.onNext(ReadOnlyPair(new, index))

        println("tracker was added")
        if (!suspendDatabaseSync)
            DatabaseManager.saveTracker(new, index)
    }

    private fun onTrackerRemoved(tracker: OTTracker, index: Int) {
        tracker.owner = null

        trackerRemoved.onNext(ReadOnlyPair(tracker, index))

        tracker.suspendDatabaseSync = true
        if (tracker.isOnShortcut) {
            tracker.isOnShortcut = false
        }

        //TODO currently, reminders are assigned to tracker so should be removed.
        val reminders = triggerManager.getAttachedTriggers(tracker, OTTrigger.ACTION_NOTIFICATION)
        for (reminder in reminders) {
            reminder.isOn = false
            triggerManager.removeTrigger(reminder)
        }

        println("tracker was removed.")
        tracker.suspendDatabaseSync = false

        if (!suspendDatabaseSync)
            DatabaseManager.removeTracker(tracker, this, true)
    }

    fun findAttributeByObjectId(trackerId: String, attributeId: String): OTAttribute<out Any>? {
        return get(trackerId)?.attributes?.unObservedList?.find { it.objectId == attributeId }
    }

    operator fun get(trackerId: String): OTTracker? {
        return trackers.unObservedList.find { it.objectId == trackerId }
    }

    fun generateNewTrackerName(context: Context): String {
        return DefaultNameGenerator.generateName(context.resources.getString(R.string.msg_new_tracker_prefix), trackers.unObservedList.map { it.name }, false)
    }

    fun getTrackerObservable(trackerId: String): Observable<OTTracker> {
        return Observable.defer {
            val tracker = get(trackerId)
            if (tracker != null) {
                println("user already contains the tracker")
                Observable.just(tracker)
            } else {

                println("user is not containing the tracker. wait")
                trackerAdded.filter {
                    pair ->
                    pair.first.objectId == trackerId
                }.map { it.first }
            }
        }
    }


    fun getTriggerObservable(triggerId: String): Observable<OTTrigger> {
        return Observable.defer {
            val trigger = triggerManager.getTriggerWithId(triggerId)
            if (trigger != null) {
                Observable.just(trigger)
            } else {
                triggerManager.triggerAdded.filter {
                    trigger ->
                    trigger.objectId == triggerId
                }
            }
        }
    }

    fun crawlAllTrackersAndTriggerAtOnce(): Single<OTUser> {
        return DatabaseManager.findTrackersOfUser(objectId).doOnNext {
            trackers ->
            println("crawled trackers")
            for (tracker in trackers) {
                if (trackers.find { it.objectId == tracker.objectId } == null)
                    this.trackers.add(tracker)
            }
        }.concatMap {
            trackers ->
            println("crawled triggers")
            DatabaseManager.findTriggersOfUser(this).doOnNext {
                triggers ->
                for (trigger in triggers) {
                    this.triggerManager.putNewTrigger(trigger)
                }
            }.map { this }
        }.first().toSingle()
    }


    fun addExampleTrackers() {
        val context = OTApplication.app
        val diaryTracker = newTracker(context.getString(R.string.msg_example_tracker_diary), true, OTTracker.CREATION_FLAG_TUTORIAL, true)
        diaryTracker.isOnShortcut = true

        diaryTracker.attributes += OTAttribute.createAttribute(diaryTracker, context.getString(R.string.msg_example_trackers_date), OTAttribute.TYPE_TIME).apply {
            this.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_DAY)
        }

        diaryTracker.attributes += OTAttribute.createAttribute(diaryTracker, context.getString(R.string.msg_example_trackers_mood), OTAttribute.TYPE_RATING).apply {
            this.setPropertyValue(OTRatingAttribute.PROPERTY_OPTIONS, kr.ac.snu.hcil.omnitrack.utils.RatingOptions().apply {
                this.allowIntermediate = true
                this.leftLabel = context.getString(R.string.msg_example_trackers_mood_very_bad)
                this.middleLabel = context.getString(R.string.msg_example_trackers_mood_normal)
                this.rightLabel = context.getString(R.string.msg_example_trackers_mood_very_good)
                this.type = kr.ac.snu.hcil.omnitrack.utils.RatingOptions.DisplayType.Likert
            })
        }

        diaryTracker.attributes += OTAttribute.createAttribute(diaryTracker, context.getString(R.string.msg_example_trackers_weather), OTAttribute.TYPE_CHOICE).apply {
            this.setPropertyValue(OTChoiceAttribute.PROPERTY_ENTRIES,
                    UniqueStringEntryList(*context.resources.getStringArray(R.array.example_trackers_weather_list)))
            this.setPropertyValue(OTChoiceAttribute.PROPERTY_MULTISELECTION, false)
        }

        diaryTracker.attributes += OTAttribute.createAttribute(diaryTracker, context.getString(R.string.msg_example_trackers_diary_content), OTAttribute.TYPE_LONG_TEXT)


        val coffeeTracker = newTracker(context.getString(R.string.msg_example_trackers_coffee), true, OTTracker.CREATION_FLAG_TUTORIAL, true)
        coffeeTracker.isOnShortcut = true
        coffeeTracker.attributes += OTAttribute.createAttribute(diaryTracker, context.getString(R.string.msg_example_trackers_coffee_drank_at), OTAttribute.TYPE_TIME).apply {
            this.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_MINUTE)
        }


        val dailyActivityTracker = newTracker(context.getString(R.string.msg_example_trackers_daily_activity), true, OTTracker.CREATION_FLAG_TUTORIAL, true)
        dailyActivityTracker.attributes += OTAttribute.createAttribute(dailyActivityTracker, context.getString(R.string.msg_example_trackers_date), OTAttribute.TYPE_TIME).apply {
            this.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_DAY)
        }

        dailyActivityTracker.attributes += (OTAttribute.createAttribute(dailyActivityTracker, context.getString(R.string.msg_example_trackers_step_count), OTAttribute.TYPE_NUMBER) as OTNumberAttribute).apply {
            numberStyle = NumberStyle().apply {
                this.unit = context.getString(R.string.msg_example_trackers_step_count_unit)
                this.unitPosition = NumberStyle.UnitPosition.Rear
                this.fractionPart = 0
            }
        }

        val sleepTimeAttribute = OTAttribute.Companion.createAttribute(dailyActivityTracker,
                context.getString(R.string.msg_example_trackers_sleep_time), OTAttribute.TYPE_TIMESPAN)
        sleepTimeAttribute.setPropertyValue(OTTimeSpanAttribute.PROPERTY_GRANULARITY, OTTimeAttribute.GRANULARITY_MINUTE)

        val sleepTimeConnection = OTConnection()
        sleepTimeConnection.source = FitbitRecentSleepTimeMeasureFactory.makeMeasure()
        sleepTimeConnection.rangedQuery = OTTimeRangeQuery(OTTimeRangeQuery.TYPE_PIVOT_TIMESTAMP, OTTimeRangeQuery.BIN_SIZE_DAY, 0)
        sleepTimeAttribute.valueConnection = sleepTimeConnection

        dailyActivityTracker.attributes += sleepTimeAttribute

        val dailyActivityTrigger = OTTimeTrigger(null, this, "", arrayOf(dailyActivityTracker.objectId), true, OTTrigger.ACTION_BACKGROUND_LOGGING, -1)
        dailyActivityTrigger.configType = OTTimeTrigger.CONFIG_TYPE_ALARM
        dailyActivityTrigger.configVariables = OTTimeTrigger.AlarmConfig.makeConfig(11, 55, Calendar.PM)
        dailyActivityTrigger.isRepeated = true
        dailyActivityTrigger.rangeVariables = OTTimeTrigger.Range.makeConfig(0b1111111)
        triggerManager.putNewTrigger(dailyActivityTrigger)


        //=====================================================================================================================================
        val foodTracker = newTracker(context.getString(R.string.msg_example_trackers_restaurant), true, OTTracker.CREATION_FLAG_TUTORIAL, true)
        foodTracker.isOnShortcut = true
        foodTracker.attributes += OTAttribute.createAttribute(foodTracker, context.getString(R.string.msg_example_trackers_restaurant_name), OTAttribute.TYPE_SHORT_TEXT)
        foodTracker.attributes += OTAttribute.createAttribute(foodTracker, context.getString(R.string.msg_example_trackers_restaurant_date), OTAttribute.TYPE_TIME).apply {
            this.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_DAY)
        }
        foodTracker.attributes += OTAttribute.createAttribute(foodTracker, context.getString(R.string.msg_example_trackers_restaurant_location), OTAttribute.TYPE_LOCATION)
        foodTracker.attributes += OTAttribute.createAttribute(foodTracker, context.getString(R.string.msg_example_trackers_restaurant_menu), OTAttribute.TYPE_SHORT_TEXT)
        foodTracker.attributes += OTAttribute.createAttribute(foodTracker, context.getString(R.string.msg_example_trackers_restaurant_photo), OTAttribute.TYPE_IMAGE)
        foodTracker.attributes += OTAttribute.createAttribute(foodTracker, context.getString(R.string.msg_example_trackers_restaurant_rating), OTAttribute.TYPE_RATING)
        foodTracker.attributes += OTAttribute.createAttribute(foodTracker, context.getString(R.string.msg_example_trackers_restaurant_review), OTAttribute.TYPE_LONG_TEXT)
    }

}