package kr.ac.snu.hcil.omnitrack.services

import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.github.salomonbrys.kotson.jsonObject
import dagger.Lazy
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncQueueDbHelper
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import org.jetbrains.anko.getStackTraceString
import org.jetbrains.anko.runOnUiThread
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 9. 26..
 */
class OTSynchronizationService : JobService() {
    companion object {
        val TAG = OTSynchronizationService::class.java.simpleName
        const val EXTRA_KEY_ONESHOT = "isOneShot"
        const val EXTRA_KEY_FULLSYNC = "fullSync"
        const val BROADCAST_ACTION_SYNCHRONIZATION_FINISHED = "${BuildConfig.APPLICATION_ID}:synchronization_finished"
        const val BROADCAST_EXTRA_ENTITY_TYPES = "entityTypes"
    }


    @Inject
    lateinit var syncManager: Lazy<OTSyncManager>

    @Inject
    lateinit var authManager: Lazy<OTAuthManager>

    @Inject
    lateinit var shortcutPanelManager: Lazy<OTShortcutPanelManager>

    @Inject
    lateinit var syncQueueDbHelper: Lazy<SyncQueueDbHelper>

    @Inject
    lateinit var eventLogger: Lazy<IEventLogger>

    private val subscriptions = CompositeDisposable()

    override fun onCreate() {
        super.onCreate()
        (application as OTAndroidApp).currentConfiguredContext.configuredAppComponent.inject(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
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

        this.eventLogger.get().logEvent(TAG, "synchronization_started")

        if (job.extras?.getBoolean(EXTRA_KEY_FULLSYNC, false) == true) {
            syncManager.get().queueFullSync(ignoreFlags = false)
            this.eventLogger.get().logEvent(TAG, "synchronization_try_fullsync")
        }

        if (authManager.get().isUserSignedIn()) {
            runOnUiThread {
                shortcutPanelManager.get().registerShortcutRefreshSubscription(authManager.get().userId
                        ?: "", TAG)
            }
        }

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
                            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(
                                    BROADCAST_ACTION_SYNCHRONIZATION_FINISHED
                            ).apply {
                                putExtra(BROADCAST_EXTRA_ENTITY_TYPES, batchData.data.map { it.first.ordinal }.toIntArray())
                            })
                        }, { err ->
                            err.printStackTrace()
                            onStopped()
                            println("synchronization failed")
                            this.eventLogger.get().logEvent(TAG, "synchronization_failed", jsonObject("error" to err.message, "stacktrace" to err.getStackTraceString()))
                            jobFinished(job, true)
                        }, {
                            onStopped()
                            println("synchronization process finished successfully.")
                            this.eventLogger.get().logEvent(TAG, "synchronization_finished")
                            jobFinished(job, false)
                        })
        )

        return fetchPendingQueueData(internalSubject)
    }

    override fun onStopJob(job: JobParameters): Boolean {
        onStopped()
        return true
    }


    fun onStopped() {
        runOnUiThread {
            shortcutPanelManager.get().unregisterShortcutRefreshSubscription(TAG)
        }
    }
}