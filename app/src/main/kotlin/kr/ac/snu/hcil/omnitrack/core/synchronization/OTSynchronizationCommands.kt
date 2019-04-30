package kr.ac.snu.hcil.omnitrack.core.synchronization

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.salomonbrys.kotson.jsonObject
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import org.jetbrains.anko.getStackTraceString
import org.jetbrains.anko.runOnUiThread
import javax.inject.Inject

class OTSynchronizationCommands(val context: Context) {

    companion object {
        val TAG = "SyncCommands"
        const val SYNC_EVENT_NAME = "data_sync"
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

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    fun createSynchronizationTask(forceFullSync: Boolean = false): Completable {
        return Completable.defer {
            if (forceFullSync) {
                syncManager.get().queueFullSync(ignoreFlags = false)
                this.eventLogger.get().logEvent(SYNC_EVENT_NAME, "synchronization_try_fullsync")
            }

            val pendingQueue = syncQueueDbHelper.get().getAggregatedData()
            if (pendingQueue != null) {
                syncManager.get().makeSynchronizationTask(pendingQueue)
                        .doOnError { err ->
                            err.printStackTrace()
                            println("synchronization failed")
                            this.eventLogger.get().logEvent(SYNC_EVENT_NAME, "synchronization_failed", jsonObject("error" to err.message, "stacktrace" to err.getStackTraceString()))
                        }
                        .doOnComplete {
                            Log.d(TAG, "doOnComplete: purge entries")
                            syncQueueDbHelper.get().purgeEntries(pendingQueue.ids)
                        }.observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe {
                            Log.d(TAG, "doOnSubscribe: start synchronization")
                            context.runOnUiThread {
                                eventLogger.get().logEvent(SYNC_EVENT_NAME, "synchronization_started")
                                if (authManager.get().isUserSignedIn()) {
                                    shortcutPanelManager.get().registerShortcutRefreshSubscription(authManager.get().userId
                                            ?: "", TAG)
                                }
                            }
                        }.doOnComplete {
                            Log.d(TAG, "doOnComplete: broadcast the sync finish")
                            broadcastFinished(pendingQueue)
                        }.doFinally {

                            Log.d(TAG, "doFinally")
                            shortcutPanelManager.get().unregisterShortcutRefreshSubscription(TAG)

                            println("synchronization process finished successfully.")
                            this.eventLogger.get().logEvent(SYNC_EVENT_NAME, "synchronization_finished")
                        }
            } else {
                Completable.complete().doOnComplete { broadcastFinished(null) }
                //Single.just(ListenableWorker.Result.success()).doAfterSuccess { broadcastFinished(null) }
            }
        }
    }

    private fun broadcastFinished(pendingQueue: SyncQueueDbHelper.AggregatedSyncQueue?) {
        println(pendingQueue)
        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(
                BROADCAST_ACTION_SYNCHRONIZATION_FINISHED
        ).apply {
            putExtra(BROADCAST_EXTRA_ENTITY_TYPES, pendingQueue?.data?.map { it.first.ordinal }?.toIntArray()
                    ?: intArrayOf())
        })
    }
}