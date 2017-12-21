package kr.ac.snu.hcil.omnitrack.core.database.local.models

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Created by younghokim on 2017. 12. 5..
 */
open class OTUserDAO : RealmObject() {
    @PrimaryKey
    var uid: String = ""

    var name: String = ""
    var photoServerPath: String = ""
    var email: String = ""

    var consentApproved: Boolean = false

    var thisDeviceLocalKey: String = ""

    var nameSynchronizedAt: Long? = null
    var nameUpdatedAt: Long = System.currentTimeMillis()

    fun toPojo(): OTUserInfoPOJO {
        return OTUserInfoPOJO(uid, name, photoServerPath, email, consentApproved, thisDeviceLocalKey)
    }

    data class OTUserInfoPOJO(
            val uid: String,
            val name: String,
            val photoServerPath: String,
            val email: String,
            val consentApproved: Boolean,
            val thisDeviceLocalKey: String
    )
}