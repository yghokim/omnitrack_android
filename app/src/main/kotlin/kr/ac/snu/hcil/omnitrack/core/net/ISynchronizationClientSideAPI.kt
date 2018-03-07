package kr.ac.snu.hcil.omnitrack.core.net

import com.google.gson.JsonObject
import io.reactivex.Completable
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncResultEntry

/**
 * Created by younghokim on 2017. 11. 4..
 */
interface ISynchronizationClientSideAPI {
    fun setTableSynchronizationFlags(type: ESyncDataType, idTimestampPair: List<SyncResultEntry>): Completable
    fun getDirtyRowsToSync(type: ESyncDataType, ignoreFlags: Boolean): Single<List<String>>
    fun setAllRowsDirty(type: ESyncDataType): Single<Long>
    fun applyServerRowsToSync(type: ESyncDataType, jsonList: List<JsonObject>): Completable

    fun getLatestSynchronizedServerTimeOf(type: ESyncDataType): Long
}