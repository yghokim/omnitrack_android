package kr.ac.snu.hcil.omnitrack.core.database.local

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey

/**
 * Created by Young-Ho on 10/9/2017.
 */
open class OTTriggerDAO : RealmObject() {

    companion object {
        const val CONDITION_TYPE_TIME: Byte = 0
        const val CONDITION_TYPE_DATA: Byte = 1
        const val CONDITION_TYPE_EVENT: Byte = 2
        const val CONDITION_TYPE_ITEM: Byte = 3

        const val ACTION_TYPE_REMIND: Byte = 0
        const val ACTION_TYPE_LOG: Byte = 1
    }

    @PrimaryKey
    var objectId: String? = null
    var alias: String = ""
    var position: Int = 0

    @Index
    var userId: String? = null

    @Index
    var conditionType: Byte = CONDITION_TYPE_TIME
    var serializedCondition: String? = null

    @Index
    var actionType: Byte = ACTION_TYPE_REMIND
    var serializedAction: String? = null


    //Device-only properties===========
    //When synchronizing them, convey them with corresponding device local ids.
    var lastTriggeredTime: Long? = null
    var isOn: Boolean = false
    //=================================

    var trackers = RealmList<OTTrackerDAO>()

    var synchronizedAt: Long? = null
    var removed: Boolean = false
    var updatedAt: Long = System.currentTimeMillis()
    var userCreatedAt: Long = System.currentTimeMillis()

}