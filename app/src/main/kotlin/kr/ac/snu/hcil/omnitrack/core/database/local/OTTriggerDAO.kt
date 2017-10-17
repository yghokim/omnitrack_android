package kr.ac.snu.hcil.omnitrack.core.database.local

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey

/**
 * Created by Young-Ho on 10/9/2017.
 */
open class OTTriggerDAO : RealmObject() {

    @PrimaryKey
    var objectId: String? = null
    var alias: String = ""
    var position: Int = 0

    @Index
    var conditionType: Int = 0

    @Index
    var actionType: Int = 0

    var serializedAction: String? = null


    //Device-only properties===========
    //When synchronizing them, convey them with corresponding device local ids.
    var lastTriggeredTime: Long? = null
    var isOn: Boolean = false
    //=================================

    var properties = RealmList<OTStringStringEntryDAO>()
    var trackers = RealmList<OTTrackerDAO>()

    var synchronizedAt: Long? = null
    var removed: Boolean = false
    var updatedAt: Long = System.currentTimeMillis()
    var userCreatedAt: Long = System.currentTimeMillis()
}