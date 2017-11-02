package kr.ac.snu.hcil.omnitrack.core.net

import com.google.gson.JsonObject
import dagger.Lazy
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTItemPOJO
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTUserRolePOJO
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncResultEntry
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider

/**
 * Created by younghokim on 2017. 9. 28..
 */
class OTOfficialServerApiController (app: OTApp) : ISynchronizationServerSideAPI, IUserReportServerAPI {

    @Inject
    lateinit var authManager: Lazy<OTAuthManager>

    init{
        app.applicationComponent.inject(this)
    }

    private val retrofit: Retrofit by lazy {

        val client = OkHttpClient.Builder().addInterceptor(object : Interceptor {
            @Throws(IOException::class)
            override fun intercept(chain: Interceptor.Chain): Response {
                val newRequest = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer " + authManager.get().authToken)
                        .build()

                println("chaining for official server: ${chain.request().url()}")

                return chain.proceed(newRequest)
            }
        }).build()

        Retrofit.Builder()
                .client(client)
                .baseUrl(BuildConfig.OMNITRACK_SYNCHRONIZATION_SERVER_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }

    private val service: OTOfficialServerService by lazy {
        retrofit.create(OTOfficialServerService::class.java)
    }

    override fun getItemsAfter(timestamp: Long): Single<List<OTItemPOJO>> {
        return service.getItemServerChanges(timestamp.toString())
                .subscribeOn(Schedulers.io())
    }

    override fun postItemsDirty(items: List<OTItemPOJO>): Single<List<SyncResultEntry>> {
        return service.postItemLocalChanges(items)
                .subscribeOn(Schedulers.io())
    }

    override fun getUserRoles(): Single<List<OTUserRolePOJO>> {
        return service.getUserRoles().subscribeOn(Schedulers.io())
    }

    override fun postUserRoleConsentResult(result: OTUserRolePOJO): Single<Boolean> {
        return service.postUserRoleConsentResult(result).subscribeOn(Schedulers.io())
    }

    override fun putDeviceInfo(info: OTDeviceInfo): Single<ISynchronizationServerSideAPI.DeviceInfoResult> {
        return service.putDeviceInfo(info).subscribeOn(Schedulers.io())
    }

    override fun sendUserReport(inquiryData: JsonObject): Single<Boolean> {
        return service.postUserReport(inquiryData).subscribeOn(Schedulers.io())
    }

    override fun removeDeviceInfo(userId: String, deviceId: String): Single<Boolean> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}