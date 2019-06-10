package kr.ac.snu.hcil.omnitrack.core.net

import com.google.gson.JsonObject
import io.reactivex.Completable
import io.reactivex.Single
import kr.ac.snu.hcil.android.common.containers.ValueWithTimestamp
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.database.models.research.ExperimentInfo
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncResultEntry
import retrofit2.http.*

/**
 * Created by younghokim on 2017. 9. 28..
 * Retrofit service interface to communicate between OmniTrack Official server
 */
interface OTOfficialServerService {

    @GET("api/research/experiment/{experimentId}/consent")
    fun getExperimentConsentInfo(@Path("experimentId") experimentId: String): Single<ISynchronizationServerSideAPI.ExperimentConsentInfo>

    @GET("api/research/experiment/{experimentId}/verify_invitation")
    fun verifyInvitationCode(@Path("experimentId") experimentId: String, @Query("invitationCode") invitationCode: String): Single<Boolean>

    @GET("api/user/auth/check_status/{experimentId}")
    fun getExperimentParticipationStatus(@Path("experimentId") experimentId: String): Single<Boolean>

    @POST("api/user/auth/authenticate")
    fun authenticate(@Body data: JsonObject): Single<ISynchronizationServerSideAPI.AuthenticationResult>

    @POST("api/user/auth/device")
    fun postDeviceInfo(@Body info: OTDeviceInfo): Single<ISynchronizationServerSideAPI.DeviceInfoResult>

    @POST("api/user/report")
    fun postUserReport(@Body data: JsonObject): Single<Boolean>

    @GET("api/batch/changes")
    fun getServerDataChanges(@Query("types[]") types: Array<ESyncDataType>, @Query("timestamps[]") timestamps: Array<Long>): Single<Map<ESyncDataType, Array<JsonObject>>>

    @POST("api/batch/changes")
    fun postLocalDataChanges(@Body parameter: Array<out ISynchronizationServerSideAPI.DirtyRowBatchParameter>): Single<Map<ESyncDataType, Array<SyncResultEntry>>>

    @POST("api/usage_logs/batch/insert")
    fun uploadUsageLogs(@Body logs: List<String>): Single<List<Long>>

    @POST("api/user/name")
    fun postUserName(@Body nameAndTimestamp: ValueWithTimestamp<String>): Single<ISynchronizationServerSideAPI.InformationUpdateResult>

    //Research
    @POST("api/research/invitation/approve")
    fun approveExperimentInvitation(@Query("invitationCode") invitationCode: String): Single<ExperimentCommandResult>

    @POST("api/research/invitation/reject")
    fun rejectExperimentInvitation(@Query("invitationCode") invitationCode: String): Completable

    @POST("api/research/experiment/{experimentId}/dropout")
    fun dropOutFromExperiment(@Path("experimentId") experimentId: String, @Body reason: DropoutBody): Single<ExperimentCommandResult>

    @GET("api/research/experiments/history")
    fun getJoinedExperiments(@Query("after") after: Long): Single<List<ExperimentInfo>>

    @GET("api/research/invitations/public")
    fun getPublicInvitations(): Single<List<ExperimentInvitation>>

    @GET("api/package/extract")
    fun getExtractedTrackingPackage(@Query("trackerIds[]") trackerIds: Array<String>, @Query("triggerIds[]") triggerIds: Array<String>): Single<String>

    @POST("api/package/temporary")
    fun postTemporaryTrackingPackage(@Body data: JsonObject): Single<String>
}