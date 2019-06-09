package kr.ac.snu.hcil.omnitrack.core.auth

import com.google.gson.JsonObject
import io.reactivex.Completable
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo

interface IAuthServerAPI {
    fun register(
            username: String,
            email: String,
            password: String,
            deviceInfo: OTDeviceInfo,
            invitationCode: String?,
            demographicData: JsonObject?): Single<OTAuthApiController.AuthResponseData>

    fun authenticate(username: String, password: String, deviceInfo: OTDeviceInfo): Single<OTAuthApiController.AuthResponseData>

    fun updateEmail(email: String): Single<OTAuthApiController.AuthResponseData>

    fun changePassword(original: String, newPassword: String): Single<OTAuthApiController.AuthResponseData>

    fun refreshToken(token: String): Single<String>

    fun signOut(deviceInfo: OTDeviceInfo): Completable

}