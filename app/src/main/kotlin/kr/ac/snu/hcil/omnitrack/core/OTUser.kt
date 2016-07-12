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

    val projectAdded = Event<Pair<OTProject, Int>>()
    val projectRemoved = Event<Pair<OTProject, Int>>()

    val projects = ObservableList<OTProject>()

    init{
        projects.elementAdded += {sender, args->
            onProjectAdded(args.first, args.second)
        }

        projects.elementRemoved += { sender, args->
            onProjectRemoved(args.first, args.second)
        }
    }

    constructor(dbObject : UserEntity) : this(dbObject.email ?: "", dbObject.name ?: "", dbObject.id){

        for(projectObj : ProjectEntity in dbObject.projects)
        {
            projects.unObservedList.add(OTProject(projectObj))
        }
    }

    private fun onProjectAdded(new: OTProject, index: Int)
    {
        new.owner = this

        projectAdded.invoke(this, Pair(new, index))
        OmniTrackApplication.app.dbHelper.add(new.makeEntity())
    }

    private fun onProjectRemoved(prj: OTProject, index: Int)
    {
        prj.owner = null

        projectRemoved.invoke(this, Pair(prj, index))
    }
}