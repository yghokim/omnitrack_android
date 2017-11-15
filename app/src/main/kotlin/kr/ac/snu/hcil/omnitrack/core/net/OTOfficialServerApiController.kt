package kr.ac.snu.hcil.omnitrack.core.net

import com.google.gson.JsonObject
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kr.ac.snu.hcil.omnitrack.core.OTUserRolePOJO
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncResultEntry
import retrofit2.Retrofit

/**
 * Created by younghokim on 2017. 9. 28..
 */
class OTOfficialServerApiController(retrofit: Retrofit) : ISynchronizationServerSideAPI, IUserReportServerAPI {

    private val service: OTOfficialServerService by lazy {
        retrofit.create(OTOfficialServerService::class.java)
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
        TODO()
    }

    override fun getRowsSynchronizedAfter(vararg batch: Pair<ESyncDataType, Long>): Single<Map<ESyncDataType, Array<JsonObject>>> {
        return service.getServerDataChanges(batch.map { it.first }.toTypedArray(), batch.map { it.second }.toTypedArray())
                .subscribeOn(Schedulers.io())
    }

    override fun postDirtyRows(vararg batch: ISynchronizationServerSideAPI.DirtyRowBatchParameter): Single<Map<ESyncDataType, Array<SyncResultEntry>>> {
        return service.postLocalDataChanges(batch).subscribeOn(Schedulers.io())
    }

}