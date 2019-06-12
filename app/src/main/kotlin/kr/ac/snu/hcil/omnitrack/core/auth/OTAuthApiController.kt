package kr.ac.snu.hcil.omnitrack.core.auth

import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.JsonObject
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST

class OTAuthApiController(retrofit: Lazy<Retrofit>) : IAuthServerAPI {

    interface AuthRetrofitService {
        //Auth

        //Register and get JWT Token
        @POST("api/user/auth/register")
        fun register(@Body data: JsonObject): Single<IAuthServerAPI.AuthResponseData>

        @POST("api/user/auth/authenticate")
        fun authenticate(@Body data: JsonObject): Single<IAuthServerAPI.AuthResponseData>

        @POST("api/user/auth/refresh_token")
        fun refreshToken(@Body data: JsonObject): Single<String>

        @POST("api/user/auth/signout")
        fun signOut(@Body data: JsonObject): Completable

        @POST("api/user/auth/update")
        fun update(@Body data: JsonObject): Single<IAuthServerAPI.AuthResponseData>

        @POST("api/user/auth/drop")
        fun dropOutFromExperiment(@Body data: JsonObject): Completable

        @POST("api/user/auth/request_password_link")
        fun requestResetPasswordLink(@Body data: JsonObject): Single<IAuthServerAPI.PasswordResetRequestResult>

    }

    private val service: AuthRetrofitService by lazy {
        retrofit.get().create(AuthRetrofitService::class.java)
    }

    override fun register(username: String, email: String, password: String, deviceInfo: OTDeviceInfo, invitationCode: String?, demographicData: JsonObject?): Single<IAuthServerAPI.AuthResponseData> {
        return service.register(jsonObject(
                "username" to username.trim(),
                "email" to email.trim(),
                "password" to password.trim(),
                "deviceInfo" to deviceInfo.convertToJson(),
                "experimentId" to BuildConfig.DEFAULT_EXPERIMENT_ID,
                "invitationCode" to invitationCode,
                "demographic" to demographicData
        )).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun authenticate(username: String, password: String, deviceInfo: OTDeviceInfo): Single<IAuthServerAPI.AuthResponseData> {
        return service.authenticate(jsonObject(
                "username" to username,
                "password" to password,
                "deviceInfo" to deviceInfo.convertToJson(),
                "experimentId" to BuildConfig.DEFAULT_EXPERIMENT_ID
        )).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun refreshToken(token: String): Single<String> {
        return service.refreshToken(jsonObject("token" to token))
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun signOut(deviceInfo: OTDeviceInfo): Completable {
        return service.signOut(jsonObject("deviceInfo" to deviceInfo.convertToJson())).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun updateEmail(email: String): Single<IAuthServerAPI.AuthResponseData> {
        return service.update(jsonObject("email" to email)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun changePassword(original: String, newPassword: String): Single<IAuthServerAPI.AuthResponseData> {
        return service.update(jsonObject("originalPassword" to original, "newPassword" to newPassword)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun dropOutFromStudy(reason: String?): Completable {
        return service.dropOutFromExperiment(jsonObject("reason" to reason)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun requestResetPassword(username: String): Single<IAuthServerAPI.PasswordResetRequestResult> {
        return service.requestResetPasswordLink(jsonObject(
                "username" to username,
                "appName" to BuildConfig.APP_NAME
        ))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

}