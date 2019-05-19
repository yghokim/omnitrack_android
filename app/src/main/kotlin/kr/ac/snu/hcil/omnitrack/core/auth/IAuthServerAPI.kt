package kr.ac.snu.hcil.omnitrack.core.auth

import androidx.annotation.Keep
import com.google.gson.JsonObject
import io.reactivex.Completable
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo

interface IAuthServerAPI {
    @Keep
    data class ServerUserInfo(val _id: String, val name: String?, val email: String, val picture: String?, val nameUpdatedAt: Long?,
                              val dataStore: JsonObject?)

    @Keep
    data class AuthenticationResult(val inserted: Boolean, val deviceLocalKey: String, val userInfo: ServerUserInfo?)

    fun register(
            username: String,
            password: String,
            deviceInfo: OTDeviceInfo,
            invitationCode: String?,
            demographicData: JsonObject?): Single<OTAuthApiController.AuthResponseData>

    fun authenticate(username: String, password: String, deviceInfo: OTDeviceInfo): Single<OTAuthApiController.AuthResponseData>

    fun refreshToken(token: String): Single<String>

    fun signOut(deviceInfo: OTDeviceInfo): Completable

}