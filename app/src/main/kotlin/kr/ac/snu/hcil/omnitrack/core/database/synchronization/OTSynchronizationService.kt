package kr.ac.snu.hcil.omnitrack.core.database.synchronization

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kr.ac.snu.hcil.omnitrack.OTApplication
import rx.schedulers.Schedulers

/**
 * Created by younghokim on 2017. 9. 26..
 */
class OTSynchronizationService : Service() {
    companion object {

        const val ACTION_PERFORM_SYNCHRONIZATION = "kr.ac.snu.hcil.omnitrack.PERFORM_SYNCHRONIZATION"
        const val INTENT_EXTRA_SYNC_DATA_TYPE = "syncDataType"

        fun makePerformSynchronizationSessionIntent(context: Context, syncDataType: ESyncDataType): Intent {
            return Intent(context, OTSynchronizationService::class.java)
                    .setAction(ACTION_PERFORM_SYNCHRONIZATION)
                    .putExtra(INTENT_EXTRA_SYNC_DATA_TYPE, syncDataType.name)
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_PERFORM_SYNCHRONIZATION -> {
                if (intent.hasExtra(INTENT_EXTRA_SYNC_DATA_TYPE)) {
                    startSynchronization(ESyncDataType.valueOf(intent.getStringExtra(INTENT_EXTRA_SYNC_DATA_TYPE)), startId)
                }
                return START_NOT_STICKY
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startSynchronization(syncDataType: ESyncDataType, startId: Int) {
        OTApplication.app.databaseManager.getLatestSynchronizedServerTimeOf(syncDataType).observeOn(Schedulers.io())
                .subscribe { serverTime ->
                    println("last synchronized server time was ${serverTime}.")
                    val newSession = SyncSession(serverTime, syncDataType, startId)
                    newSession.performSync().subscribe { result ->
                        println(result.second)
                        stopSelf(startId)
                    }
                }
    }
}