package kr.ac.snu.hcil.omnitrack.core.database.synchronization.official

import kr.ac.snu.hcil.omnitrack.core.backend.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTItemPOJO
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.ISynchronizationServerSideAPI
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncResultEntry
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import rx.Single
import rx.schedulers.Schedulers

/**
 * Created by younghokim on 2017. 9. 28..
 */
class OTOfficialServerApiController : ISynchronizationServerSideAPI {
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
                .baseUrl("http://147.46.242.28:3000")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }

    private val service: OTOfficialServerService by lazy {
        retrofit.create(OTOfficialServerService::class.java)
    }

    override fun getItemsAfter(timestamp: Long): Single<List<OTItemPOJO>> {
        return service.getItemServerChanges(OTAuthManager.userId!!, timestamp.toString())
                .observeOn(Schedulers.io())
    }

    override fun postItemsDirty(items: List<OTItemPOJO>): Single<List<SyncResultEntry>> {
        return service.postItemLocalChanges(OTAuthManager.userId!!, items)
                .observeOn(Schedulers.io())
    }
}