package kr.ac.snu.hcil.omnitrack.core.triggers

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker

/**
 * Created by younghokim on 16. 7. 27..
 */
class OTPeriodicTrigger : OTTrigger {

    override val typeId: Int = TYPE_PERIODIC

    override val typeNameResourceId: Int = R.string.trigger_periodic_name
    override val descriptionResourceId: Int = R.string.trigger_periodic_desc

    var period: Long by properties
    var pivot: Long by properties

    constructor(name: String, tracker: OTTracker) : super(name, tracker)
    constructor(objectId: String?, dbId: Long?, name: String, trackerObjectId: String, serializedProperties: String?=null) : super(objectId, dbId, name, trackerObjectId)

    override fun handleActivationOnSystem(context: Context) {

    }

    override fun handleOn() {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun handleOff() {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}