package kr.ac.snu.hcil.omnitrack.services

import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncQueueDbHelper
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationClientSideAPI
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 9. 26..
 */
class OTSynchronizationService : JobService() {

    companion object {
        val TAG = OTSynchronizationService::class.java.simpleName

        const val EXTRA_KEY_ONESHOT = "isOneShot"
    }

    @Inject
    lateinit var syncClient: Lazy<ISynchronizationClientSideAPI>

    @Inject
    lateinit var syncServerController: Lazy<ISynchronizationServerSideAPI>

    @Inject
    lateinit var syncQueueDbHelper: Lazy<SyncQueueDbHelper>

    private val subscriptions = CompositeDisposable()

    override fun onCreate() {
        super.onCreate()
        (application as OTApp).synchronizationComponent.inject(this)
    }

    override fun onStopJob(job: JobParameters): Boolean {
        //accidental finish
        subscriptions.clear()
        return true
    }

    private fun fetchPendingQueueData(internalSubject: PublishSubject<SyncQueueDbHelper.AggregatedSyncQueue>): Boolean {
        val pendingQueue = syncQueueDbHelper.get().getAggregatedData()
        if (pendingQueue != null) {
            internalSubject.onNext(pendingQueue)
            println("synchQueue data is pending. update internal subject.")
            return true
        } else {
            internalSubject.onComplete()
            println("synchQueue is empty. complete the internal subject.")
            return false
        }
    }

    override fun onStartJob(job: JobParameters): Boolean {
        println("start synchronization... ${job.extras}")
        val internalSubject = PublishSubject.create<SyncQueueDbHelper.AggregatedSyncQueue>()

        subscriptions.add(
                internalSubject.doAfterNext { batchData ->
                    syncQueueDbHelper.get().purgeEntries(batchData.ids)
                    fetchPendingQueueData(internalSubject)
                }.concatMap { batchData ->
                    startSynchronization(batchData).toSingle { batchData }.toObservable()
                }
                        .subscribe({ batchData ->
                            println(batchData)
                        }, { err ->
                            err.printStackTrace()
                            jobFinished(job, true)
                        }, {
                            jobFinished(job, false)
                        })
        )

        return fetchPendingQueueData(internalSubject)
    }


    private fun startSynchronization(batchData: SyncQueueDbHelper.AggregatedSyncQueue): Completable {
        val downDirection = Completable.defer {
            val dbLatestTimestamps = batchData.data.filter { it.second.contains(SyncDirection.DOWNLOAD) }
                    .map { (type, _) ->
                        Pair(type, syncClient.get().getLatestSynchronizedServerTimeOf(type))
                    }.toTypedArray()
            if (dbLatestTimestamps.isNotEmpty()) {
                syncServerController.get().getRowsSynchronizedAfter(*dbLatestTimestamps).flatMapCompletable { serverItemsMap ->
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
                syncServerController.get().postDirtyRows(*it.map { entry -> ISynchronizationServerSideAPI.DirtyRowBatchParameter(entry.first, entry.second.toTypedArray()) }.toTypedArray())
            }.flatMapCompletable { result ->
                Completable.merge(result.map { entry -> syncClient.get().setTableSynchronizationFlags(entry.key, entry.value.toList()) })
            }
        }

        return downDirection.andThen(upDirection)
    }
}