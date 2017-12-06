package kr.ac.snu.hcil.omnitrack.core.net

import android.support.annotation.Keep
import com.google.gson.JsonObject
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.OTUserRolePOJO
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncResultEntry

/**
 * Created by younghokim on 2017. 9. 27..
 */
interface ISynchronizationServerSideAPI {


    @Keep
    data class DeviceInfoResult(var result: String, var deviceLocalKey: String?, val payloads: Map<String, String>? = null)

    @Keep
    data class DirtyRowBatchParameter(val type: ESyncDataType, val rows: Array<String>)

    fun getUserRoles(): Single<List<OTUserRolePOJO>>

    fun postUserRoleConsentResult(result: OTUserRolePOJO): Single<Boolean>

    fun putDeviceInfo(info: OTDeviceInfo): Single<DeviceInfoResult>
    fun removeDeviceInfo(userId: String, deviceId: String): Single<Boolean>

    //=================================================================================================================================
    //server returns server-side changes after designated timestamp.
    fun getRowsSynchronizedAfter(vararg batch: Pair<ESyncDataType, Long>): Single<Map<ESyncDataType, Array<JsonObject>>>

    //send dirty data to server
    fun postDirtyRows(vararg batch: DirtyRowBatchParameter): Single<Map<ESyncDataType, Array<SyncResultEntry>>>
    //==================================================================================================================================
}