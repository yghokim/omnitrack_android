package kr.ac.snu.hcil.omnitrack.core.net

import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTUserRolePOJO
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncResultEntry
import org.json.JSONObject

/**
 * Created by younghokim on 2017. 9. 27..
 */
interface ISynchronizationServerSideAPI {

    data class DeviceInfoResult(var result: String, var deviceLocalKey: String?)

    data class DirtyRowBatchParameter(val type: ESyncDataType, val rows: Array<String>)

    fun getUserRoles(): Single<List<OTUserRolePOJO>>

    fun postUserRoleConsentResult(result: OTUserRolePOJO): Single<Boolean>

    fun putDeviceInfo(info: OTDeviceInfo): Single<DeviceInfoResult>
    fun removeDeviceInfo(userId: String, deviceId: String): Single<Boolean>

    //=================================================================================================================================
    //server returns server-side changes after designated timestamp.
    fun getRowsSynchronizedAfter(vararg batch: Pair<ESyncDataType, Long>): Single<Map<ESyncDataType, Array<JSONObject>>>

    //send dirty data to server
    fun postDirtyRows(vararg batch: DirtyRowBatchParameter): Single<Map<ESyncDataType, Array<SyncResultEntry>>>
    //==================================================================================================================================
}