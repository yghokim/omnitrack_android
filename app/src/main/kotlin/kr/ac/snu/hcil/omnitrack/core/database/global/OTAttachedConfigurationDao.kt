package kr.ac.snu.hcil.omnitrack.core.database.global

import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import kr.ac.snu.hcil.omnitrack.core.configuration.OTConfiguration

/**
 * Created by younghokim on 2017. 12. 18..
 */
open class OTAttachedConfigurationDao : RealmObject() {
    @PrimaryKey
    var id: String = ""

    @Index
    var firebaseInstanceId: String? = null
    var firebaseInstanceIdCreatedAt: Long? = null

    @Index
    var firebaseCloudMessagingSenderId: String? = null

    @Required
    var dataJson: String = "{}"

    var updatedAt: Long = System.currentTimeMillis()
    var createdAt: Long = updatedAt

    @Ignore
    private var _configuration: OTConfiguration? = null

    fun staticConfiguration(): OTConfiguration {
        if (_configuration == null) {
            _configuration = OTConfiguration(dataJson)
        }
        return _configuration!!
    }

    companion object {
        const val FIELD_INSTANCE_ID = "firebaseInstanceId"
        const val FIELD_GCM_SENDER_ID = "firebaseCloudMessagingSenderId"
        const val FIELD_ID = "id"
    }
}