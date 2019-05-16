package kr.ac.snu.hcil.omnitrack.core.auth

import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.JsonObject
import dagger.Lazy
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
        fun register(@Body data: JsonObject): Single<String>

        @POST("api/user/auth/authenticate")
        fun authenticate(@Body data: JsonObject): Single<String>

        @POST("api/user/auth/signout")
        fun signOut(@Body data: JsonObject): Single<String>
    }

    private val service: AuthRetrofitService by lazy {
        retrofit.get().create(AuthRetrofitService::class.java)
    }

    override fun register(email: String, password: String, deviceInfo: OTDeviceInfo, invitationCode: String?, demographicData: JsonObject?): Single<String> {
        return service.register(jsonObject(
                "email" to email.trim(),
                "password" to email.trim(),
                "deviceInfo" to deviceInfo.convertToJson(),
                "experimentId" to BuildConfig.DEFAULT_EXPERIMENT_ID,
                "invitationCode" to invitationCode,
                "demographic" to demographicData
        )).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun authenticate(email: String, password: String, deviceInfo: OTDeviceInfo): Single<String> {
        return service.authenticate(jsonObject(
                "email" to email,
                "password" to password,
                "deviceInfo" to deviceInfo
        ))
    }
}