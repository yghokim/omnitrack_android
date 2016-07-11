package kr.ac.snu.hcil.omnitrack.core

import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*

/**
 * Created by Young-Ho on 7/11/2016.
 */
class Project(id: String?, name: String) : UniqueObject(id, name) {

    private var trackers: ArrayList<Tracker> = ArrayList<Tracker>()

    //Public properties
    val trackerAddedEvent = Event<Pair<Tracker, Int>>()
    val trackerRemovedEvent = Event<Pair<Tracker, Int>>()
    val trackerIndexChangedEvent = Event<Pair<Tracker, Int>>()


    constructor() : this(null, "New Project")

    open fun addTracker(tracker: Tracker): Boolean{
        if(trackers.add(tracker)) {
            onTrackerAdded(tracker, trackers.size - 1)
            return true
        }
        else{
            return false
        }
    }

    open fun removeTracker(tracker: Tracker): Boolean{
        val index = trackers.indexOf(tracker)
        if(index >= 0)
        {
            trackers.removeAt(index)
            onTrackerRemoved(tracker, index)
            return true
        }
        else{
            return false
        }
    }

    private fun onTrackerAdded(tracker:Tracker, index: Int)
    {
        trackerAddedEvent.invoke(this, Pair(tracker, index));
    }

    private fun onTrackerRemoved(tracker:Tracker, index: Int)
    {
        trackerRemovedEvent.invoke(this, Pair(tracker, index));
    }

}