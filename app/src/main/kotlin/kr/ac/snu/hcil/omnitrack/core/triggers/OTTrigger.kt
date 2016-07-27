package kr.ac.snu.hcil.omnitrack.core.triggers

import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.core.NamedObject
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.database.IDatabaseStorable
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by younghokim on 16. 7. 27..
 */
abstract class OTTrigger : NamedObject {

    companion object {
        const val ACTION_NOTIFICATION = 0
        const val ACTION_BACKGROUND_LOGGING = 1
    }

    val trackerObjectId: String

    val tracker: OTTracker

    var action: Int by Delegates.observable(ACTION_NOTIFICATION)
    {
        prop, old, new ->

    }
    var isActive: Boolean by Delegates.observable(true) {
        prop, old, new ->

    }

    protected val properties = HashMap<String, Any?>()


    constructor(name: String, tracker: OTTracker) : super(null, null, name) {
        trackerObjectId = tracker.objectId
        this.tracker = tracker
    }

    constructor(objectId: String, dbId: Long, name: String, trackerObjectId: String) : super(objectId, dbId, name) {
        this.trackerObjectId = trackerObjectId
        this.tracker = OmniTrackApplication.app.currentUser[trackerObjectId]!!
    }


}