package kr.ac.snu.hcil.omnitrack.services

import com.firebase.jobdispatcher.JobParameters
import dagger.Lazy
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncQueueDbHelper
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.utils.ConfigurableJobService
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 9. 26..
 */
class OTSynchronizationService : ConfigurableJobService() {
    companion object {
        val TAG = OTSynchronizationService::class.java.simpleName
        const val EXTRA_KEY_ONESHOT = "isOneShot"
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

            if (authManager.get().isUserSignedIn() && configuredContext.isActive) {
                shortcutPanelManager.get().registerShortcutRefreshSubscription(authManager.get().userId ?: "", "$TAG/${configuredContext.configuration.id}")
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
                                jobFinished(job, true)
                            }, {
                                onStopped()
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
            shortcutPanelManager.get().unregisterShortcutRefreshSubscription("$TAG/${configuredContext.configuration.id}")
        }

    }

    override fun makeNewTask(configuredContext: ConfiguredContext): IConfiguredTask {
        return ConfiguredTask(configuredContext)
    }
}