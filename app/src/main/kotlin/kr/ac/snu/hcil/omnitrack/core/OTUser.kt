package kr.ac.snu.hcil.omnitrack.core

import android.content.Context
import android.content.SharedPreferences
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.database.DatabaseHelper
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerManager
import kr.ac.snu.hcil.omnitrack.utils.DefaultNameGenerator
import kr.ac.snu.hcil.omnitrack.utils.ObservableList
import kr.ac.snu.hcil.omnitrack.utils.ReadOnlyPair
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */
class OTUser(val objectId: String, var name: String, var photoUrl: String?, _trackers: List<OTTracker>? = null) {

    companion object {

        const val PREFERENCES_KEY_OBJECT_ID = "ot_user_object_id"
        const val PREFERENCES_KEY_NAME = "ot_user_name"
        const val PREFERENCES_KEY_PHOTO_URL = "ot_user_photo_url"


        //const val PREFERENCES_KEY_EMAIL = "user_email"

        fun loadCachedInstance(sp: SharedPreferences, databaseHelper: DatabaseHelper): OTUser? {
            if (sp.contains(PREFERENCES_KEY_OBJECT_ID)) {
                val objId = sp.getString(PREFERENCES_KEY_OBJECT_ID, null)
                val name = sp.getString(PREFERENCES_KEY_NAME, null)
                val photoUrl = sp.getString(PREFERENCES_KEY_PHOTO_URL, null)
                if (objId != null && name != null) {
                    return OTUser(objId, name, photoUrl, databaseHelper.findTrackersOfUser(objId))
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

    var isDatasetDirtySinceLastSync = false


    /*
    val email: String by Delegates.observable(email) {
        prop, old, new ->
        if (old != new) {
            isDirtySinceLastSync = true
        }
    }*/

    val trackers = ObservableList<OTTracker>()


    val triggerManager: OTTriggerManager

    private val _removedTrackerIds = ArrayList<Long>()
    fun fetchRemovedTrackerIds(): LongArray {
        val result = _removedTrackerIds.toLongArray()
        _removedTrackerIds.clear()
        return result;
    }

    val trackerAdded = Event<ReadOnlyPair<OTTracker, Int>>()
    val trackerRemoved = Event<ReadOnlyPair<OTTracker, Int>>()
    val trackerIndexChanged = Event<IntRange>()


    init {

        if (_trackers != null) {
            for (tracker: OTTracker in _trackers) {
                trackers.unObservedList.add(tracker)

                tracker.addedToUser.suspend = true
                tracker.owner = this
                tracker.addedToUser.suspend = false

            }
        }

        triggerManager = OTTriggerManager(this, OTApplication.app.dbHelper.findTriggersOfUser(this))

        trackers.elementAdded += { sender, args ->
            onTrackerAdded(args.first, args.second)
        }

        trackers.elementRemoved += { sender, args ->
            onTrackerRemoved(args.first, args.second)
        }

        trackers.elementReordered += {
            sender, range ->
            for (i in range) {
                trackers[i].isDirtySinceLastSync = true
            }
        }
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
    }

    fun newTrackerWithDefaultName(context: Context, add: Boolean): OTTracker {
        return newTracker(this.generateNewTrackerName(context), add)
    }

    fun newTracker(name: String, add: Boolean): OTTracker {
        val tracker = OTTracker(name)
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
        _removedTrackerIds.remove(new.dbId)

        trackerAdded.invoke(this, ReadOnlyPair(new, index))
    }

    private fun onTrackerRemoved(tracker: OTTracker, index: Int) {
        tracker.owner = null

        if (tracker.dbId != null)
            _removedTrackerIds.add(tracker.dbId as Long)

        trackerRemoved.invoke(this, ReadOnlyPair(tracker, index))

        if (tracker.isOnShortcut) {
            tracker.isOnShortcut = false
        }

        //TODO currently, reminders are assigned to tracker so should be removed.
        val reminders = triggerManager.getAttachedTriggers(tracker, OTTrigger.ACTION_NOTIFICATION)
        for (reminder in reminders) {
            reminder.isOn = false
            triggerManager.removeTrigger(reminder)
        }
    }

    fun findAttributeByObjectId(id: String): OTAttribute<out Any>? {

        for (tracker in trackers) {
            val result = tracker.attributes.unObservedList.find { it.objectId == id }
            if (result != null) {
                return result;
            } else continue
        }
        return null
    }

    operator fun get(trackerId: String): OTTracker? {
        return trackers.unObservedList.find { it.objectId == trackerId }
    }

    fun generateNewTrackerName(context: Context): String {
        return DefaultNameGenerator.generateName(context.resources.getString(R.string.msg_new_tracker_prefix), trackers.unObservedList.map { it.name })
    }
}