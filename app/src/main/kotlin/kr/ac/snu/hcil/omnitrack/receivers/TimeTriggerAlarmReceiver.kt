package kr.ac.snu.hcil.omnitrack.receivers

import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.SparseArray
import io.reactivex.disposables.CompositeDisposable
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.triggers.ITriggerAlarmController
import kr.ac.snu.hcil.omnitrack.core.triggers.OTReminderCommands
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerAlarmManager
import kr.ac.snu.hcil.omnitrack.services.OTReminderService
import javax.inject.Inject

/**
 * Created by Young-Ho Kim on 2016-10-11.
 */
class TimeTriggerAlarmReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "TimeTriggerAlarmReceiver"
        private val EXTRA_WAKE_LOCK_ID = "${BuildConfig.APPLICATION_ID}.wakelockid"

        private val mActiveWakeLocks = SparseArray<PowerManager.WakeLock>()
        private var mNextId = 1

        fun startWakefulService(context: Context, intent: Intent): ComponentName? {
            synchronized(mActiveWakeLocks) {
                val id = mNextId
                mNextId++
                if (mNextId <= 0) {
                    mNextId = 1
                }

                intent.putExtra(EXTRA_WAKE_LOCK_ID, id)
                val comp = context.startService(intent) ?: return null

                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        "wake:" + comp.flattenToShortString())
                wl.setReferenceCounted(false)
                wl.acquire(100000) // remove timeout
                mActiveWakeLocks.put(id, wl)
                return comp
            }
        }

        fun completeWakefulIntent(intent: Intent): Boolean {
            val id = intent.getIntExtra(EXTRA_WAKE_LOCK_ID, 0)
            if (id == 0) {
                return false
            }
            synchronized(mActiveWakeLocks) {
                val wl = mActiveWakeLocks.get(id)
                if (wl != null) {
                    wl.release()
                    mActiveWakeLocks.remove(id)
                    return true
                }
                // We return true whether or not we actually found the wake lock
                // the return code is defined to indicate whether the Intent contained
                // an identifier for a wake lock that it was supposed to match.
                // We just log a warning here if there is no wake lock found, which could
                // happen for example if this function is called twice on the same
                // intent or the process is killed and restarted before processing the intent.
                return true
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, TimeTriggerWakefulHandlingService::class.java)
        serviceIntent.action = intent.action
        serviceIntent.putExtras(intent)
        OTApp.logger.writeSystemLog("Start wakeful service - ${intent.action}", TAG)
        startWakefulService(context, serviceIntent)
    }

    class TimeTriggerWakefulHandlingService : Service() {

        private val creationSubscriptions = CompositeDisposable()

        override fun onBind(p0: Intent?): IBinder {
            TODO()
        }

        @Inject
        protected lateinit var triggerAlarmController: ITriggerAlarmController

        override fun onCreate() {
            super.onCreate()
            (application as OTAndroidApp).triggerSystemComponent.inject(this)
        }

        override fun onDestroy() {
            super.onDestroy()
            creationSubscriptions.clear()
        }

        override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
            OTApp.logger.writeSystemLog("Wakeful Trigger Alarm Service onStartCommand", TAG)
            when (intent.action) {
                OTApp.BROADCAST_ACTION_TIME_TRIGGER_ALARM -> {
                    val alarmId = intent.getIntExtra(OTTriggerAlarmManager.INTENT_EXTRA_ALARM_ID, -1)

                    creationSubscriptions.add(
                            triggerAlarmController.onAlarmFired(alarmId).doAfterTerminate {
                                completeWakefulIntent(intent)
                                OTApp.logger.writeSystemLog("Released wake lock for AlarmReceiver.", TAG)
                                stopSelf(startId)
                            }.subscribe({
                                println("successfully handled fired alarm: $alarmId")
                                OTApp.logger.writeSystemLog("successfully handled fired alarm: $alarmId", TAG)
                            }, { err ->
                                println("trigger alarm handling error")
                                err.printStackTrace()
                            })
                    )
                }
                OTApp.BROADCAST_ACTION_REMINDER_AUTO_EXPIRY_ALARM -> {
                    OTApp.logger.writeSystemLog("Handle auto expiry alarm", TAG)
                    val entryId = intent.getLongExtra(OTReminderService.INTENT_EXTRA_ENTRY_ID, -1)
                    //this.applicationContext.startService(OTReminderService.makeReminderDismissedIntent(this.applicationContext, configId, triggerId, entryId))
                    val commands = OTReminderCommands(applicationContext)

                    val realm = commands.getNewRealm()
                    commands.dismissSyncImpl(entryId, realm)
                    realm.close()
                    OTApp.logger.writeSystemLog("Successfully dismissed reminder by alarm. entryId: $entryId", TAG)

                    completeWakefulIntent(intent)
                    OTApp.logger.writeSystemLog("Released wake lock for AlarmReceiver.", TAG)
                    stopSelf(startId)
                }
            }

            return START_NOT_STICKY
        }
    }
}