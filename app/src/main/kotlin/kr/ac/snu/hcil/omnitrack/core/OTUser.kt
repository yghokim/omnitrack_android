package kr.ac.snu.hcil.omnitrack.core

import android.content.Context
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.utils.DefaultNameGenerator
import kr.ac.snu.hcil.omnitrack.utils.ObservableList
import kr.ac.snu.hcil.omnitrack.utils.ReadOnlyPair
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */
class OTUser(objectId: String?, dbId: Long?, name: String, email: String, attributeIdSeed: Long = 0, _trackers: List<OTTracker>? = null) : NamedObject(objectId, dbId, name) {


    var attributeIdSeed: Long = attributeIdSeed
        private set(value) {
            if (field != value) {
                isDirtySinceLastSync = true
                field = value
            }
        }


    val email: String by Delegates.observable(email) {
        prop, old, new ->
        if (old != new) {
            isDirtySinceLastSync = true
        }
    }

    val trackers = ObservableList<OTTracker>()

    private val _removedTrackerIds = ArrayList<Long>()
    fun fetchRemovedTrackerIds(): LongArray {
        val result = _removedTrackerIds.toLongArray()
        _removedTrackerIds.clear()
        return result;
    }

    val trackerAdded = Event<ReadOnlyPair<OTTracker, Int>>()
    val trackerRemoved = Event<ReadOnlyPair<OTTracker, Int>>()
    val trackerIndexChanged = Event<IntRange>()

    constructor(name: String, email: String) : this(null, null, name, email) {

    }

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

    fun newTrackerWithDefaultName(context: Context, add: Boolean): OTTracker {
        return newTracker(OTApplication.app.currentUser.generateNewTrackerName(context), add)
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

        tracker.color = if (unOccupied.size > 0) {
            unOccupied.first()
        } else {
            OTApplication.app.colorPalette.first()
        }

        if (add) {
            trackers.add(tracker)
        }

        return tracker
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

    fun getNewAttributeObjectId(): Long {
        return ++attributeIdSeed
    }

    fun generateNewTrackerName(context: Context): String {
        return DefaultNameGenerator.generateName("${context.resources.getString(R.string.msg_new_tracker_prefix)}", trackers.unObservedList.map { it.name })
    }
}