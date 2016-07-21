package kr.ac.snu.hcil.omnitrack.core

import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.utils.ObservableList
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.io.Serializable
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */
class OTUser(objectId: String?, dbId: Long?, name: String, email: String, attributeIdSeed: Long = 0, _trackers: List<OTTracker>? = null) : UniqueObject(objectId, dbId, name) {


    var attributeIdSeed: Long = attributeIdSeed
        private set


    val email: String by Delegates.observable(email) {
        prop, old, new ->

    }

    val trackers = ObservableList<OTTracker>()

    private val _removedTrackerIds = ArrayList<Long>()
    fun fetchRemovedTrackerIds(): Array<Long> {
        val result = _removedTrackerIds.toTypedArray()
        _removedTrackerIds.clear()
        return result;
    }

    val trackerAdded = Event<Pair<OTTracker, Int>>()
    val trackerRemoved = Event<Pair<OTTracker, Int>>()
    val trackerIndexChanged = Event<Pair<OTTracker, Int>>()

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
    }

    private fun onTrackerAdded(new: OTTracker, index: Int) {
        new.owner = this
        _removedTrackerIds.remove(new.dbId)

        trackerAdded.invoke(this, Pair(new, index))
    }

    private fun onTrackerRemoved(tracker: OTTracker, index: Int) {
        tracker.owner = null

        if (tracker.dbId != null)
            _removedTrackerIds.add(tracker.dbId as Long)

        trackerRemoved.invoke(this, Pair(tracker, index))
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

    fun getNewAttributeObjectId(): Long {
        return ++attributeIdSeed
    }
}