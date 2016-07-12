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
class OTUser(objectId: String?, dbId: Long?, name: String, email: String, _projects: List<OTProject>?) : UniqueObject(objectId, dbId, name) {

    val email: String by Delegates.observable(email){
        prop, old, new->

    }

    val projectAdded = Event<Pair<OTProject, Int>>()
    val projectRemoved = Event<Pair<OTProject, Int>>()

    val projects = ObservableList<OTProject>()

    private val _removedProjectIds = ArrayList<Long>()
    fun fetchRemovedProjectIds() : Array<Long>
    {
        val result = _removedProjectIds.toTypedArray()
        _removedProjectIds.clear()
        return result;
    }



    constructor(name: String, email: String) : this(null, null, name, email, null){

    }

    init{

        if(_projects != null) {
            for (project: OTProject in _projects) {
                projects.unObservedList.add(project)

                project.addedToUser.suspend = true
                project.owner = this
                project.addedToUser.suspend = false

            }
        }

        projects.elementAdded += {sender, args->
            onProjectAdded(args.first, args.second)
        }

        projects.elementRemoved += { sender, args->
            onProjectRemoved(args.first, args.second)
        }
    }

    private fun onProjectAdded(new: OTProject, index: Int)
    {
        new.owner = this

        projectAdded.invoke(this, Pair(new, index))
    }

    private fun onProjectRemoved(prj: OTProject, index: Int)
    {
        prj.owner = null

        if(prj.dbId!= null)
            _removedProjectIds.add(prj.dbId as Long)

        projectRemoved.invoke(this, Pair(prj, index))
    }
}