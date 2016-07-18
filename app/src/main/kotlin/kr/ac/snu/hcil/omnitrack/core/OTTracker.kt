package kr.ac.snu.hcil.omnitrack.core

//import kr.ac.snu.hcil.omnitrack.core.database.TrackerEntity
import android.graphics.Color
import kr.ac.snu.hcil.omnitrack.utils.ObservableList
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 7/11/2016.
 */
class OTTracker(objectId:String?, dbId: Long?, name: String, color: Int=Color.WHITE) : UniqueObject(objectId, dbId, name) {
    private val attributes = ObservableList<OTAttribute>()

    var owner: OTUser? by Delegates.observable(null as OTUser?){
        prop, old, new ->
        if (old != null) {
            removedFromUser.invoke(this, old)
        }
        if (new != null) {
            addedToUser.invoke(this, new)
        }
    }

    var color: Int = color

    val removedFromUser = Event<OTUser>()
    val addedToUser = Event<OTUser>()


    constructor(): this(null, null, "New Tracker")

    constructor(name: String): this(null,null, name)

    /*
    constructor(dbObj: TrackerEntity): this(dbObj.objectId, dbObj.id.value, dbObj.name)
    {

    }
*/
    init{
        attributes.elementAdded += {
            sender, args->

        }

        attributes.elementRemoved += {
            sender, args->

        }
    }


}