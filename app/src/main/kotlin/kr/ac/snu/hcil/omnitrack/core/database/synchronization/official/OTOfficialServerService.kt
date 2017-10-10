package kr.ac.snu.hcil.omnitrack.core.database.synchronization.official

import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTItemPOJO
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTUserRolePOJO
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.ISynchronizationServerSideAPI
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncResultEntry
import retrofit2.http.*
import rx.Single

/**
 * Created by younghokim on 2017. 9. 28..
 * Retrofit service interface to communicate between OmniTrack Official server
 */
interface OTOfficialServerService {

    @GET("api/items/changes")
    fun getItemServerChanges(@Query("timestamp") timestamp: String): Single<List<OTItemPOJO>>

    @POST("api/items/changes")
    fun postItemLocalChanges(@Body list: List<OTItemPOJO>): Single<List<SyncResultEntry>>

    @GET("api/user/roles")
    fun getUserRoles(): Single<List<OTUserRolePOJO>>

    @POST("api/user/role")
    fun postUserRoleConsentResult(@Body data: OTUserRolePOJO): Single<Boolean>

    @PUT("api/user/device")
    fun putDeviceInfo(@Body info: OTDeviceInfo): Single<ISynchronizationServerSideAPI.DeviceInfoResult>


}