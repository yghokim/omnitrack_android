package kr.ac.snu.hcil.omnitrack.core.database.configured.models

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

    var thisDeviceLocalKey: String = ""

    var nameSynchronizedAt: Long? = null
    var nameUpdatedAt: Long = System.currentTimeMillis()

}