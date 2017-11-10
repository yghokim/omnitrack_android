package kr.ac.snu.hcil.omnitrack.services

import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import dagger.Lazy
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationClientSideAPI
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncQueueDbHelper
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
    lateinit var syncManager: Lazy<OTSyncManager>

    @Inject
    lateinit var syncClient: Lazy<ISynchronizationClientSideAPI>

    @Inject
    lateinit var syncServerController: Lazy<ISynchronizationServerSideAPI>

    @Inject
    lateinit var syncQueueDbHelper: Lazy<SyncQueueDbHelper>

    private val subscriptions = CompositeDisposable()

    override fun onCreate() {
        super.onCreate()
        (application as OTApp).applicationComponent.inject(this)
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
                    syncManager.get().makeSynchronizationTask(batchData).toSingle { batchData }.toObservable()
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

}