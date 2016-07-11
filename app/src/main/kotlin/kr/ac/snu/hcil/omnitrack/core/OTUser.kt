package kr.ac.snu.hcil.omnitrack.core

import kr.ac.snu.hcil.omnitrack.core.database.ProjectEntity
import kr.ac.snu.hcil.omnitrack.core.database.UserEntity
import kr.ac.snu.hcil.omnitrack.utils.ObservableList
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */
class OTUser(email: String, name: String, val dbId: Long?) {
    val email: String by Delegates.observable(email){
        prop, old, new->

    }
    val name: String by Delegates.observable(name){
        prop, old, new->

    }

    val projectAddedEvent = Event<Pair<OTProject, Int>>()
    val projectRemovedEvent = Event<Pair<OTProject, Int>>()

    private val projects = ObservableList<OTProject>()

    init{
        projects.elementAdded += {sender, args->
            onProjectAdded(args.first, args.second)
        }

        projects.elementRemoved += { sender, args->
            onProjectRemoved(args.first, args.second)
        }
    }

    constructor(dbObject : UserEntity) : this(dbObject.email ?: "", dbObject.name ?: "", dbObject.id){
        /*
        for(projectObj : ProjectEntity in dbObject.projects)
        {
            projects.unObservedList.add(OTProject(projectObj))
        }*/
    }

    fun addProject(new : OTProject): Boolean
    {
        return projects.add(new)
    }

    fun removeProject(prj: OTProject): Boolean
    {
        return projects.remove(prj)
    }

    private fun onProjectAdded(new: OTProject, index: Int)
    {
        projectAddedEvent.invoke(this, Pair(new, index))
    }

    private fun onProjectRemoved(prj: OTProject, index: Int)
    {
        projectRemovedEvent.invoke(this, Pair(prj, index))
    }
}