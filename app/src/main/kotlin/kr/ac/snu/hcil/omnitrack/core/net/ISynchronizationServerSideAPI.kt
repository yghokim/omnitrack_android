package kr.ac.snu.hcil.omnitrack.core.net

import android.support.annotation.Keep
import com.google.gson.JsonObject
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncResultEntry

/**
 * Created by younghokim on 2017. 9. 27..
 */
interface ISynchronizationServerSideAPI {

    @Keep
    data class ServerUserInfo(val _id: String, val name: String?, val email: String, val picture: String?, val nameUpdatedAt: Long?,
                              val dataStore: JsonObject?)

    @Keep
    data class ExperimentConsentInfo(val receiveConsentInApp: Boolean, val consent: String?, val demographicFormSchema: JsonObject?)

    @Keep
    data class AuthenticationResult(val inserted: Boolean, val deviceLocalKey: String, val userInfo: ServerUserInfo?)

    @Keep
    data class DeviceInfoResult(var result: String, var deviceLocalKey: String?)

    @Keep
    data class InformationUpdateResult(var success: Boolean, var finalValue: String, val payloads: Map<String, String>? = null)

    @Keep
    data class DirtyRowBatchParameter(val type: ESyncDataType, val rows: Array<String>)

    //New authentication APIs======================================
    fun checkExperimentParticipationStatus(experimentId: String): Single<Boolean>

    fun authenticate(deviceInfo: OTDeviceInfo, invitationCode: String?, demographicData: JsonObject?): Single<AuthenticationResult>
    fun getExperimentConsentInfo(experimentId: String): Single<ExperimentConsentInfo>
    fun verifyInvitationCode(invitationCode: String, experimentId: String): Single<Boolean>
    //=============================================================

    fun putDeviceInfo(info: OTDeviceInfo): Single<DeviceInfoResult>
    fun removeDeviceInfo(userId: String, deviceId: String): Single<Boolean>

    fun putUserName(name: String, timestamp: Long): Single<InformationUpdateResult>

    fun getTrackingPackageJson(trackerIds: Array<String>, triggerIds: Array<String>): Single<String>
    fun uploadTemporaryTrackingPackage(packageString: String): Single<String>

    //=================================================================================================================================
    //server returns server-side changes after designated timestamp.
    fun getRowsSynchronizedAfter(vararg batch: Pair<ESyncDataType, Long>): Single<Map<ESyncDataType, Array<JsonObject>>>

    //send dirty data to server
    fun postDirtyRows(vararg batch: DirtyRowBatchParameter): Single<Map<ESyncDataType, Array<SyncResultEntry>>>
    //==================================================================================================================================
}