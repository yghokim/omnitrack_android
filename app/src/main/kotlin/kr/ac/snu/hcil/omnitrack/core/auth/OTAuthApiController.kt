package kr.ac.snu.hcil.omnitrack.core.auth

import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.JsonObject
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.internal.Keep
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST

class OTAuthApiController(retrofit: Lazy<Retrofit>) : IAuthServerAPI {

    @Keep
    data class AuthResponseData(val token: String, val deviceLocalKey: String?)

    interface AuthRetrofitService {
        //Auth

        //Register and get JWT Token
        @POST("api/user/auth/register")
        fun register(@Body data: JsonObject): Single<AuthResponseData>

        @POST("api/user/auth/authenticate")
        fun authenticate(@Body data: JsonObject): Single<AuthResponseData>

        @POST("api/user/auth/refresh_token")
        fun refreshToken(@Body data: JsonObject): Single<String>

        @POST("api/user/auth/signout")
        fun signOut(@Body data: JsonObject): Completable
    }

    private val service: AuthRetrofitService by lazy {
        retrofit.get().create(AuthRetrofitService::class.java)
    }

    override fun register(username: String, password: String, deviceInfo: OTDeviceInfo, invitationCode: String?, demographicData: JsonObject?): Single<AuthResponseData> {
        return service.register(jsonObject(
                "username" to username.trim(),
                "password" to password.trim(),
                "deviceInfo" to deviceInfo.convertToJson(),
                "experimentId" to BuildConfig.DEFAULT_EXPERIMENT_ID,
                "invitationCode" to invitationCode,
                "demographic" to demographicData
        )).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun authenticate(username: String, password: String, deviceInfo: OTDeviceInfo): Single<AuthResponseData> {
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
}