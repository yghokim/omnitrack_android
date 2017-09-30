package kr.ac.snu.hcil.omnitrack.core.database.synchronization

import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTItemPOJO
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTUserRolePOJO
import rx.Single

/**
 * Created by younghokim on 2017. 9. 27..
 */
interface ISynchronizationServerSideAPI {
    //server returns server-side changes after designated timestamp.
    fun getItemsAfter(timestamp: Long): Single<List<OTItemPOJO>>

    //server receives
    fun postItemsDirty(items: List<OTItemPOJO>): Single<List<SyncResultEntry>>

    fun getUserRoles(): Single<List<OTUserRolePOJO>>

    fun postUserRoleConsentResult(result: OTUserRolePOJO): Single<Boolean>

    fun putDeviceInfo(info: OTDeviceInfo): Single<Boolean>
    fun removeDeviceInfo(userId: String, deviceId: String): Single<Boolean>
}