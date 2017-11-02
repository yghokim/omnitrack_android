package kr.ac.snu.hcil.omnitrack.core.database.synchronization

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import dagger.Lazy
import io.reactivex.schedulers.Schedulers
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 9. 26..
 */
class OTSynchronizationService : Service() {
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

    override fun onCreate() {
        super.onCreate()
        (application as OTApp).applicationComponent.inject(this)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_PERFORM_SYNCHRONIZATION -> {
                if (intent.hasExtra(INTENT_EXTRA_SYNC_DATA_TYPE)) {
                    startSynchronization(
                            ESyncDataType.valueOf(intent.getStringExtra(INTENT_EXTRA_SYNC_DATA_TYPE)),
                            try {
                                SyncDirection.valueOf(intent.getStringExtra(INTENT_EXTRA_SYNC_DIRECTION))
                            } catch (ex: Exception) {
                                ex.printStackTrace(); SyncDirection.BIDIRECTIONAL
                            },
                            startId)
                }
                return START_NOT_STICKY
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startSynchronization(syncDataType: ESyncDataType, direction: SyncDirection, startId: Int) {
        dbManager.get().getLatestSynchronizedServerTimeOf(syncDataType).observeOn(Schedulers.io())
                .subscribe { serverTime ->
                    println("last synchronized server time was ${serverTime}.")
                    val newSession = SyncSession(serverTime, syncDataType, direction, startId)
                    newSession.performSync().subscribe { (session, success) ->
                        println(success)
                        stopSelf(startId)
                    }
                }
    }
}