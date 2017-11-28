package kr.ac.snu.hcil.omnitrack.core.net

import com.google.gson.JsonObject
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.OTUserRolePOJO
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncResultEntry
import retrofit2.http.*

/**
 * Created by younghokim on 2017. 9. 28..
 * Retrofit service interface to communicate between OmniTrack Official server
 */
interface OTOfficialServerService {
    @GET("api/user/roles")
    fun getUserRoles(): Single<List<OTUserRolePOJO>>

    @POST("api/user/role")
    fun postUserRoleConsentResult(@Body data: OTUserRolePOJO): Single<Boolean>

    @PUT("api/user/device")
    fun putDeviceInfo(@Body info: OTDeviceInfo): Single<ISynchronizationServerSideAPI.DeviceInfoResult>

    @POST("api/user/report")
    fun postUserReport(@Body data: JsonObject): Single<Boolean>

    @GET("api/batch/changes")
    fun getServerDataChanges(@Query("types[]") types: Array<ESyncDataType>, @Query("timestamps[]") timestamps: Array<Long>): Single<Map<ESyncDataType, Array<JsonObject>>>

    @POST("api/batch/changes")
    fun postLocalDataChanges(@Body parameter: Array<out ISynchronizationServerSideAPI.DirtyRowBatchParameter>): Single<Map<ESyncDataType, Array<SyncResultEntry>>>

    @POST("api/usage_logs/batch/insert")
    fun uploadUsageLogs(@Body logs: List<String>): Single<List<Long>>
}