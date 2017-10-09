package kr.ac.snu.hcil.omnitrack.core.database.local

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey

/**
 * Created by Young-Ho on 10/9/2017.
 */
class OTTriggerDAO : RealmObject() {

    @PrimaryKey
    var objectId: String? = null
    var alias: String = ""
    var position: Int = 0

    @Index
    var action: Int = 0

    @Index
    var type: Int = 0

    var properties = RealmList<OTStringStringEntryDAO>()
    var lastTriggeredTime: Long? = null
    var trackers = RealmList<OTTrackerDAO>()
}