package kr.ac.snu.hcil.omnitrack.core.triggers

import kr.ac.snu.hcil.omnitrack.core.OTTracker

/**
 * Created by younghokim on 16. 7. 27..
 */
class OTPeriodicTrigger : OTTrigger {

    var period: Long by properties
    var pivot: Long by properties

    constructor(name: String, tracker: OTTracker) : super(name, tracker)
    constructor(objectId: String, dbId: Long, name: String, trackerObjectId: String) : super(objectId, dbId, name, trackerObjectId)


}