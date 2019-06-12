package kr.ac.snu.hcil.omnitrack.core.auth

import com.google.gson.JsonObject
import io.reactivex.Completable
import io.reactivex.Single
import io.realm.internal.Keep
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo

interface IAuthServerAPI {

    @Keep
    data class AuthResponseData(val token: String, val deviceLocalKey: String?, val appFlags: JsonObject?)

    @Keep
    data class PasswordResetRequestResult(val success: Boolean, val email: String)

    fun register(
            username: String,
            email: String,
            password: String,
            deviceInfo: OTDeviceInfo,
            invitationCode: String?,
            demographicData: JsonObject?): Single<AuthResponseData>

    fun authenticate(username: String, password: String, deviceInfo: OTDeviceInfo): Single<AuthResponseData>

    fun updateEmail(email: String): Single<AuthResponseData>

    fun changePassword(original: String, newPassword: String): Single<AuthResponseData>

    fun refreshToken(token: String): Single<String>

    fun signOut(deviceInfo: OTDeviceInfo): Completable

    fun requestResetPassword(username: String): Single<PasswordResetRequestResult>

    fun dropOutFromStudy(reason: String?): Completable

}