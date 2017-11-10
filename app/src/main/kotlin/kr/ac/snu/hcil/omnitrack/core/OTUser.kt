package kr.ac.snu.hcil.omnitrack.core

import android.content.Context
import android.content.SharedPreferences
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.utils.DefaultNameGenerator
import kr.ac.snu.hcil.omnitrack.utils.ObservableList
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetUpdateService
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */
class OTUser(val objectId: String, var name: String?, var photoUrl: String?, _trackers: List<OTTracker>? = null) {

    companion object {

        const val TAG = "OTUser"

        const val PREFERENCES_KEY_OBJECT_ID = "ot_user_object_id"
        const val PREFERENCES_KEY_NAME = "ot_user_name"
        const val PREFERENCES_KEY_PHOTO_URL = "ot_user_photo_url"
        const val PREFERENCES_KEY_CONSENT_APPROVED = "ot_user_consent_approved"


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
                    .remove(PREFERENCES_KEY_CONSENT_APPROVED)
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

    val trackers = ObservableList<OTTracker>()

    //val trackerAdded = Event<ReadOnlyPair<OTTracker, Int>>()

    /*
    private var trackerListDbReference: DatabaseReference? = null
    private val trackerListChangeEventListener: ChildEventListener

    private var triggerListDbReference: DatabaseReference? = null
    private val triggerListChangeEventListener: ChildEventListener
    */

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

        trackers.elementAdded += { sender, args ->
            onTrackerAdded(args.first, args.second)
        }

        trackers.elementRemoved += { _, args ->
            onTrackerRemoved(args.first, args.second)
        }

        trackers.elementReordered += {
            sender, range ->
            for (i in range) {
                trackers[i].intrinsicPosition = i
                //trackers[i].databasePointRef?.child("position")?.setValue(i)
            }
        }

        trackers.listModified += {
            _, args ->
            OTApp.instance.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(OTApp.instance))
        }

        //trackerListDbReference = databaseRef?.child(DatabaseManager.CHILD_NAME_TRACKERS)
        //triggerListDbReference = databaseRef?.child(DatabaseManager.CHILD_NAME_TRIGGERS)

        /*
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
        */

    }

    fun getTrackersOnShortcut(): List<OTTracker> {
        return trackers.filter { it.isOnShortcut == true }
    }

    /*
    fun getRecentTrackers(): List<OTTracker>{
        val list = trackers.unObservedList.toMutableList()
        list.map { OTApp.instance.dbHelper.getLastLoggingTimeAsync(it,) }
    }*/

    fun detachFromSystem() {
        //trackerListDbReference?.removeEventListener(trackerListChangeEventListener)
        //triggerListDbReference?.removeEventListener(triggerListChangeEventListener)

    }

    fun newTrackerWithDefaultName(context: Context, add: Boolean): OTTracker {
        return newTracker(this.generateNewTrackerName(context), add, isEditable = true)
    }

    fun newTracker(name: String, add: Boolean, creationFlags: Map<String, String>? = null, isEditable: Boolean = true): OTTracker {
        val tracker = OTTracker(null, name, isOnShortcut = false, isEditable = isEditable, creationFlags = creationFlags)
        val unOccupied = OTApp.instance.colorPalette.filter {
            color ->
            trackers.unObservedList.find {
                it ->
                it.color == color
            } == null
        }

        tracker.color = if (unOccupied.isNotEmpty()) {
            unOccupied.first()
        } else {
            OTApp.instance.colorPalette.first()
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
            //OTShortcutPanelManager += new
        }

        //trackerAdded.onNext(ReadOnlyPair(new, index))

        println("tracker was added")
        if (!suspendDatabaseSync) {
            //OTApp.instance.databaseManager.saveTracker(new, index)
        }
    }

    private fun onTrackerRemoved(tracker: OTTracker, index: Int) {
        tracker.owner = null

        //trackerRemoved.onNext(ReadOnlyPair(tracker, index))

        tracker.suspendDatabaseSync = true
        if (tracker.isOnShortcut) {
            tracker.isOnShortcut = false
        }

        println("tracker was removed.")
        tracker.suspendDatabaseSync = false

        if (!suspendDatabaseSync) {
            //OTApp.instance.databaseManager.removeTracker(tracker, this, true)
        }
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

    /*
    fun addExampleTrackers() {
        val context = OTApp.instance
        val diaryTracker = newTracker(context.getString(R.string.msg_example_tracker_diary), true, OTTracker.CREATION_FLAG_TUTORIAL, true)
        diaryTracker.isOnShortcut = true

        diaryTracker.attributes += OTAttribute.createAttribute(diaryTracker, context.getString(R.string.msg_example_trackers_date), OTAttributeManager.TYPE_TIME).apply {
            this.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_DAY)
        }

        diaryTracker.attributes += OTAttribute.createAttribute(diaryTracker, context.getString(R.string.msg_example_trackers_mood), OTAttributeManager.TYPE_RATING).apply {
            this.setPropertyValue(OTRatingAttribute.PROPERTY_OPTIONS, kr.ac.snu.hcil.omnitrack.utils.RatingOptions().apply {
                this.allowIntermediate = true
                this.leftLabel = context.getString(R.string.msg_example_trackers_mood_very_bad)
                this.middleLabel = context.getString(R.string.msg_example_trackers_mood_normal)
                this.rightLabel = context.getString(R.string.msg_example_trackers_mood_very_good)
                this.type = kr.ac.snu.hcil.omnitrack.utils.RatingOptions.DisplayType.Likert
            })
        }

        diaryTracker.attributes += OTAttribute.createAttribute(diaryTracker, context.getString(R.string.msg_example_trackers_weather), OTAttributeManager.TYPE_CHOICE).apply {
            this.setPropertyValue(OTChoiceAttribute.PROPERTY_ENTRIES,
                    UniqueStringEntryList(*context.resources.getStringArray(R.array.example_trackers_weather_list)))
            this.setPropertyValue(OTChoiceAttribute.PROPERTY_MULTISELECTION, false)
        }

        diaryTracker.attributes += OTAttribute.createAttribute(diaryTracker, context.getString(R.string.msg_example_trackers_diary_content), OTAttributeManager.TYPE_LONG_TEXT)


        val coffeeTracker = newTracker(context.getString(R.string.msg_example_trackers_coffee), true, OTTracker.CREATION_FLAG_TUTORIAL, true)
        coffeeTracker.isOnShortcut = true
        coffeeTracker.attributes += OTAttribute.createAttribute(diaryTracker, context.getString(R.string.msg_example_trackers_coffee_drank_at), OTAttributeManager.TYPE_TIME).apply {
            this.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_MINUTE)
        }


        val dailyActivityTracker = newTracker(context.getString(R.string.msg_example_trackers_daily_activity), true, OTTracker.CREATION_FLAG_TUTORIAL, true)
        dailyActivityTracker.attributes += OTAttribute.createAttribute(dailyActivityTracker, context.getString(R.string.msg_example_trackers_date), OTAttributeManager.TYPE_TIME).apply {
            this.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_DAY)
        }

        dailyActivityTracker.attributes += (OTAttribute.createAttribute(dailyActivityTracker, context.getString(R.string.msg_example_trackers_step_count), OTAttributeManager.TYPE_NUMBER) as OTNumberAttribute).apply {
            numberStyle = NumberStyle().apply {
                this.unit = context.getString(R.string.msg_example_trackers_step_count_unit)
                this.unitPosition = NumberStyle.UnitPosition.Rear
                this.fractionPart = 0
            }
        }

        val sleepTimeAttribute = OTAttribute.Companion.createAttribute(dailyActivityTracker,
                context.getString(R.string.msg_example_trackers_sleep_time), OTAttributeManager.TYPE_TIMESPAN)
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
        foodTracker.attributes += OTAttribute.createAttribute(foodTracker, context.getString(R.string.msg_example_trackers_restaurant_name), OTAttributeManager.TYPE_SHORT_TEXT)
        foodTracker.attributes += OTAttribute.createAttribute(foodTracker, context.getString(R.string.msg_example_trackers_restaurant_date), OTAttributeManager.TYPE_TIME).apply {
            this.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_DAY)
        }
        foodTracker.attributes += OTAttribute.createAttribute(foodTracker, context.getString(R.string.msg_example_trackers_restaurant_location), OTAttributeManager.TYPE_LOCATION)
        foodTracker.attributes += OTAttribute.createAttribute(foodTracker, context.getString(R.string.msg_example_trackers_restaurant_menu), OTAttributeManager.TYPE_SHORT_TEXT)
        foodTracker.attributes += OTAttribute.createAttribute(foodTracker, context.getString(R.string.msg_example_trackers_restaurant_photo), OTAttributeManager.TYPE_IMAGE)
        foodTracker.attributes += OTAttribute.createAttribute(foodTracker, context.getString(R.string.msg_example_trackers_restaurant_rating), OTAttributeManager.TYPE_RATING)
        foodTracker.attributes += OTAttribute.createAttribute(foodTracker, context.getString(R.string.msg_example_trackers_restaurant_review), OTAttributeManager.TYPE_LONG_TEXT)
    }*/

}