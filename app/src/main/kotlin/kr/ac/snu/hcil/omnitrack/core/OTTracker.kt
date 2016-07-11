package kr.ac.snu.hcil.omnitrack.core

//import kr.ac.snu.hcil.omnitrack.core.database.TrackerEntity
import kr.ac.snu.hcil.omnitrack.utils.ObservableList
import java.util.*

/**
 * Created by Young-Ho on 7/11/2016.
 */
class OTTracker(objectId:String?, dbId: Long?, name: String) : UniqueObject(objectId, dbId, name) {
    private var attributes = ObservableList<OTAttribute>()

    constructor(): this(null, null, "New Tracker")
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