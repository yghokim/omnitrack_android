package kr.ac.snu.hcil.omnitrack.core

import kr.ac.snu.hcil.omnitrack.core.database.ProjectEntity
import kr.ac.snu.hcil.omnitrack.utils.ObservableList
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*

/**
 * Created by Young-Ho on 7/11/2016.
 */
class OTProject(objectId: String?, dbId: Long?,  name: String) : UniqueObject(objectId, dbId, name) {

    private var trackers = ObservableList<OTTracker>()

    //Public properties
    val trackerAddedEvent = Event<Pair<OTTracker, Int>>()
    val trackerRemovedEvent = Event<Pair<OTTracker, Int>>()
    val trackerIndexChangedEvent = Event<Pair<OTTracker, Int>>()

    constructor() : this(null, null, "New Project")

    constructor(dbObject: ProjectEntity) : this(dbObject.objectId, dbObject.id, dbObject.name ?: "Noname")
    {
        /*
        for( trackerDbObj: TrackerEntity in dbObject.trackers ) {
            trackers.unObservedList.add(OTTracker(trackerDbObj))
        }*/
    }

    init{
        trackers.elementAdded += {
            sender, args->
                onTrackerAdded(args.first, args.second)
        }

        trackers.elementRemoved += {
            sender, args->
                onTrackerRemoved(args.first, args.second)
        }
    }

    open fun addTracker(tracker: OTTracker): Boolean{
        return trackers.add(tracker)
    }

    open fun removeTracker(tracker: OTTracker): Boolean{
        return trackers.remove(tracker)
    }

    private fun onTrackerAdded(tracker: OTTracker, index: Int)
    {
        trackerAddedEvent.invoke(this, Pair(tracker, index));
    }

    private fun onTrackerRemoved(tracker: OTTracker, index: Int)
    {
        trackerRemovedEvent.invoke(this, Pair(tracker, index));
    }

}