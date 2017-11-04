package kr.ac.snu.hcil.omnitrack.core.net

import android.net.Uri
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTItemPOJO
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTUserRolePOJO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncResultEntry

/**
 * Created by younghokim on 2017. 9. 27..
 */
interface ISynchronizationServerSideAPI {

    data class DeviceInfoResult(var result: String, var deviceLocalKey: String?)

    fun getUserRoles(): Single<List<OTUserRolePOJO>>

    fun postUserRoleConsentResult(result: OTUserRolePOJO): Single<Boolean>

    fun putDeviceInfo(info: OTDeviceInfo): Single<DeviceInfoResult>
    fun removeDeviceInfo(userId: String, deviceId: String): Single<Boolean>


    //server returns server-side changes after designated timestamp.
    fun getItemsAfter(timestamp: Long): Single<List<OTItemDAO>>

    //server receives
    fun postItemsDirty(items: List<OTItemDAO>): Single<List<SyncResultEntry>>

    fun getTrackersAfter(timestamp: Long): Single<List<OTTrackerDAO>>

    fun postTrackersDirty(trackers: List<OTTrackerDAO>) : Single<List<SyncResultEntry>>

    fun getTriggersAfter(timestamp: Long): Single<List<OTTriggerDAO>>

    fun postTriggersDirty(triggers: List<OTTriggerDAO>): Single<List<SyncResultEntry>>

}