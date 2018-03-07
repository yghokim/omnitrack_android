package kr.ac.snu.hcil.omnitrack.services

import com.firebase.jobdispatcher.JobParameters
import com.github.salomonbrys.kotson.jsonObject
import dagger.Lazy
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncQueueDbHelper
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.utils.ConfigurableJobService
import org.jetbrains.anko.getStackTraceString
import org.jetbrains.anko.runOnUiThread
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 9. 26..
 */
class OTSynchronizationService : ConfigurableJobService() {
    companion object {
        val TAG = OTSynchronizationService::class.java.simpleName
        const val EXTRA_KEY_ONESHOT = "isOneShot"
        const val EXTRA_KEY_FULLSYNC = "fullSync"
    }

    inner class ConfiguredTask(val configuredContext: ConfiguredContext) : IConfiguredTask {

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

        init {
            configuredContext.configuredAppComponent.inject(this)
        }

        override fun dispose() {
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

            if (authManager.get().isUserSignedIn() && configuredContext.isActive) {
                runOnUiThread {
                    shortcutPanelManager.get().registerShortcutRefreshSubscription(authManager.get().userId ?: "", "$TAG/${configuredContext.configuration.id}")
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
                shortcutPanelManager.get().unregisterShortcutRefreshSubscription("$TAG/${configuredContext.configuration.id}")
            }
        }

    }

    override fun makeNewTask(configuredContext: ConfiguredContext): IConfiguredTask {
        return ConfiguredTask(configuredContext)
    }
}