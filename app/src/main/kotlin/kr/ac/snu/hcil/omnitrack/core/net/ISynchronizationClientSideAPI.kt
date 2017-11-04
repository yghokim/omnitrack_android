package kr.ac.snu.hcil.omnitrack.core.net

import io.reactivex.Completable
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTItemPOJO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncResultEntry
import org.json.JSONObject

/**
 * Created by younghokim on 2017. 11. 4..
 */
interface ISynchronizationClientSideAPI {
    fun setTableSynchronizationFlags(type: ESyncDataType, idTimestampPair: List<SyncResultEntry>): Completable
    fun getDirtyRowsToSync(type: ESyncDataType): Single<List<String>>
    fun applyServerRowsToSync(type: ESyncDataType, jsonList: List<String>): Completable

    fun getLatestSynchronizedServerTimeOf(type: ESyncDataType): Single<Long>
}