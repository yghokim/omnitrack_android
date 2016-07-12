package kr.ac.snu.hcil.omnitrack.core

import android.support.annotation.Nullable
import kr.ac.snu.hcil.omnitrack.utils.ObservableList
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 7/11/2016.
 */
class OTProject(objectId: String?, dbId: Long?,  name: String, _trackers: ArrayList<OTTracker>?=null) : UniqueObject(objectId, dbId, name) {

    private var trackers = ObservableList<OTTracker>()

    var owner: OTUser? by Delegates.observable(null as OTUser?){
        prop, old, new ->
            if (old != null) {
                removedFromUser.invoke(this, old)
            }
            if (new != null) {
                addedToUser.invoke(this, new)
            }
    }

    //Public properties
    val trackerAdded = Event<Pair<OTTracker, Int>>()
    val trackerRemoved = Event<Pair<OTTracker, Int>>()
    val trackerIndexChanged = Event<Pair<OTTracker, Int>>()
    val removedFromUser = Event<OTUser>()
    val addedToUser = Event<OTUser>()

    constructor() : this(null, null, "New Project")

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

    override fun onNameChanged(newName: String)
    {
        super.onNameChanged(newName)
    }

    open fun addTracker(tracker: OTTracker): Boolean{
        return trackers.add(tracker)
    }

    open fun removeTracker(tracker: OTTracker): Boolean{
        return trackers.remove(tracker)
    }

    private fun onTrackerAdded(tracker: OTTracker, index: Int)
    {
        trackerAdded.invoke(this, Pair(tracker, index));
    }

    private fun onTrackerRemoved(tracker: OTTracker, index: Int)
    {
        trackerRemoved.invoke(this, Pair(tracker, index));
    }

}