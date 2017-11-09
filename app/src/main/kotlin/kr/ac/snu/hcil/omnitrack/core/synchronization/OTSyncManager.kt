package kr.ac.snu.hcil.omnitrack.core.synchronization

import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.Job
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.di.ApplicationScope
import kr.ac.snu.hcil.omnitrack.core.di.ServerSyncOneShot
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationClientSideAPI
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
import javax.inject.Inject
import javax.inject.Provider

/**
 * Created by younghokim on 2017. 9. 27..
 */
@ApplicationScope
class OTSyncManager @Inject constructor(
        private val syncQueueDbHelper: SyncQueueDbHelper,
        private val syncClient: Lazy<ISynchronizationClientSideAPI>,
        private val syncServer: Lazy<ISynchronizationServerSideAPI>,
        @ServerSyncOneShot private val oneShotJobProvider: Provider<Job>,
        private val dispatcher: Lazy<FirebaseJobDispatcher>) {

    fun registerSyncQueue(type: ESyncDataType, direction: SyncDirection, reserveService: Boolean = true) {
        syncQueueDbHelper.insertNewEntry(type, direction, System.currentTimeMillis())
        if (reserveService) {
            reserveSyncServiceNow()
        }
    }

    fun queueFullSync(direction: SyncDirection = SyncDirection.BIDIRECTIONAL) {
        ESyncDataType.values().forEach { type ->
            syncQueueDbHelper.insertNewEntry(type, SyncDirection.BIDIRECTIONAL, System.currentTimeMillis())
        }
    }

    fun reserveSyncServiceNow() {
        dispatcher.get().mustSchedule(oneShotJobProvider.get())
    }



    fun makeSynchronizationTask(batchData: SyncQueueDbHelper.AggregatedSyncQueue): Completable {
        val downDirection = Completable.defer {
            val dbLatestTimestamps = batchData.data.filter { it.second.contains(SyncDirection.DOWNLOAD) }
                    .map { (type, _) ->
                        Pair(type, syncClient.get().getLatestSynchronizedServerTimeOf(type))
                    }.toTypedArray()
            if (dbLatestTimestamps.isNotEmpty()) {
                syncServer.get().getRowsSynchronizedAfter(*dbLatestTimestamps).flatMapCompletable { serverItemsMap ->
                    println(serverItemsMap)
                    Completable.merge(
                            serverItemsMap.map { (type, serverItems) ->
                                if (serverItems.isNotEmpty()) {
                                    syncClient.get().applyServerRowsToSync(type, serverItems.toList())
                                } else {
                                    Completable.complete()
                                }
                            }
                    )
                }
            } else {
                return@defer Completable.complete()
            }
        }

        val upDirection = Completable.defer {
            val downTypes = batchData.data.filter { it.second.contains(SyncDirection.UPLOAD) }.map { it.first }.toTypedArray()
            println("start sync download from server: ${downTypes.joinToString(",")}")
            if (downTypes.isEmpty()) {
                return@defer Completable.complete()
            }

            return@defer Single.zip(downTypes.map { type -> syncClient.get().getDirtyRowsToSync(type).map { Pair(type, it) } }) { clientDirtyRowsArray ->
                clientDirtyRowsArray.map { it as Pair<ESyncDataType, List<String>> }
            }.flatMap {
                println("sync send dirty rows to server: ${it}")
                syncServer.get().postDirtyRows(*it.map { entry -> ISynchronizationServerSideAPI.DirtyRowBatchParameter(entry.first, entry.second.toTypedArray()) }.toTypedArray())
            }.flatMapCompletable { result ->
                Completable.merge(result.map { entry -> syncClient.get().setTableSynchronizationFlags(entry.key, entry.value.toList()) })
            }
        }

        return downDirection.andThen(upDirection)
    }
}