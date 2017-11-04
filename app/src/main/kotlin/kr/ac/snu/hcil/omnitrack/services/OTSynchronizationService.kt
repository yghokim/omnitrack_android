package kr.ac.snu.hcil.omnitrack.services

import android.content.Context
import android.content.Intent
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.CompletableSource
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncQueueDbHelper
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncSession
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 9. 26..
 */
class OTSynchronizationService : JobService() {

    companion object {

        const val ACTION_PERFORM_SYNCHRONIZATION = "${OTApp.PREFIX_ACTION}.PERFORM_SYNCHRONIZATION"
        const val INTENT_EXTRA_SYNC_DATA_TYPE = "syncDataType"
        const val INTENT_EXTRA_SYNC_DIRECTION = "syncDirection"

        fun makePerformSynchronizationSessionIntent(context: Context, syncDataType: ESyncDataType, syncDirection: SyncDirection): Intent {
            return Intent(context, OTSynchronizationService::class.java)
                    .setAction(ACTION_PERFORM_SYNCHRONIZATION)
                    .putExtra(INTENT_EXTRA_SYNC_DATA_TYPE, syncDataType.name)
                    .putExtra(INTENT_EXTRA_SYNC_DIRECTION, syncDirection.name)
        }
    }

    @Inject
    lateinit var dbManager: Lazy<RealmDatabaseManager>

    @Inject
    lateinit var syncQueueDbHelper: Lazy<SyncQueueDbHelper>

    private val subscriptions = CompositeDisposable()

    override fun onCreate() {
        super.onCreate()
        (application as OTApp).synchronizationComponent.inject(this)
    }

    override fun onStopJob(job: JobParameters): Boolean {
        subscriptions.clear()
        return true
    }

    private fun fetchPendingQueueData(internalSubject: PublishSubject<SyncQueueDbHelper.AggregatedSyncQueue>): Boolean {
        val pendingQueue = syncQueueDbHelper.get().getAggregatedData()
        if(pendingQueue!=null) {
            internalSubject.onNext(pendingQueue)
            return true
        }
        else {
            internalSubject.onComplete()
            return false
        }
    }

    override fun onStartJob(job: JobParameters): Boolean {
        val internalSubject = PublishSubject.create<SyncQueueDbHelper.AggregatedSyncQueue>()

        subscriptions.add(
                internalSubject.doAfterNext{
                    batchData->
                    syncQueueDbHelper.get().purgeEntries(batchData.ids)
                    fetchPendingQueueData(internalSubject)
                }.subscribe({
                    batchData->
                    startSynchronization(batchData)
                }, {
                    jobFinished(job, true)
                }, {
                    jobFinished(job, true)
                })
        )

        return fetchPendingQueueData(internalSubject)
    }

    private fun startSynchronization(batchData: SyncQueueDbHelper.AggregatedSyncQueue): Completable {
        return Completable.complete()
    }

    private fun startSynchronization(syncDataType: ESyncDataType, direction: SyncDirection): Completable {
        return dbManager.get().getLatestSynchronizedServerTimeOf(syncDataType).observeOn(Schedulers.io())
                .flatMapCompletable {
                    serverTime->
                    println("last synchronized server time was ${serverTime}.")
                    val newSession = SyncSession(serverTime, syncDataType, direction)
                    return@flatMapCompletable newSession.performSync().flatMapCompletable { (session, success)-> if(success) Completable.complete() else Completable.error(Exception("Sync failed")) } }
                }
    }
}