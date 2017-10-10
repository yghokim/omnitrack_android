package kr.ac.snu.hcil.omnitrack.core.database.synchronization.official

import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTItemPOJO
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTUserRolePOJO
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.ISynchronizationServerSideAPI
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncResultEntry
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import rx.Single
import rx.schedulers.Schedulers
import java.io.IOException

/**
 * Created by younghokim on 2017. 9. 28..
 */
class OTOfficialServerApiController : ISynchronizationServerSideAPI {

    override fun removeDeviceInfo(userId: String, deviceId: String): Single<Boolean> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val retrofit: Retrofit by lazy {

        val client = OkHttpClient.Builder().addInterceptor(object : Interceptor {
            @Throws(IOException::class)
            override fun intercept(chain: Interceptor.Chain): Response {
                val newRequest = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer " + OTAuthManager.authToken)
                        .build()

                println("chaining for official server: ${chain.request().url()}")

                return chain.proceed(newRequest)
            }
        }).build()

        Retrofit.Builder()
                .client(client)
                .baseUrl("http://147.46.242.28:3000") // test server
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
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
}