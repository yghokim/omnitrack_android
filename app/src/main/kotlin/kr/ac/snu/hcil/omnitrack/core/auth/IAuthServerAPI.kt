package kr.ac.snu.hcil.omnitrack.core.auth

import androidx.annotation.Keep
import com.google.gson.JsonObject
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo

interface IAuthServerAPI {
    @Keep
    data class ServerUserInfo(val _id: String, val name: String?, val email: String, val picture: String?, val nameUpdatedAt: Long?,
                              val dataStore: JsonObject?)

    @Keep
    data class AuthenticationResult(val inserted: Boolean, val deviceLocalKey: String, val userInfo: ServerUserInfo?)

    fun register(
            email: String,
            password: String,
            deviceInfo: OTDeviceInfo,
            invitationCode: String?,
            demographicData: JsonObject?): Single<String>

    fun authenticate(email: String, password: String, deviceInfo: OTDeviceInfo): Single<String>
}