package kr.ac.snu.hcil.omnitrack.receivers

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import io.reactivex.disposables.CompositeDisposable
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.triggers.ITriggerAlarmController
import kr.ac.snu.hcil.omnitrack.core.triggers.OTReminderCommands
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerAlarmManager
import kr.ac.snu.hcil.omnitrack.services.OTReminderService
import kr.ac.snu.hcil.omnitrack.utils.system.WakefulBroadcastReceiverStaticLock
import javax.inject.Inject

/**
 * Created by Young-Ho Kim on 2016-10-11.
 */
class TimeTriggerAlarmReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "TimeTriggerAlarmReceiver"

        val lockImpl = WakefulBroadcastReceiverStaticLock()


    }

    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, TimeTriggerWakefulHandlingService::class.java)
        serviceIntent.action = intent.action
        serviceIntent.putExtras(intent)
        OTApp.logger.writeSystemLog("Start wakeful service - ${intent.action}", TAG)
        lockImpl.startWakefulService(context, serviceIntent)
    }

    class TimeTriggerWakefulHandlingService : Service() {

        private val creationSubscriptions = CompositeDisposable()

        override fun onBind(p0: Intent?): IBinder? {
            return null
        }

        @Inject
        protected lateinit var triggerAlarmController: ITriggerAlarmController

        override fun onCreate() {
            super.onCreate()
            (application as OTAndroidApp).applicationComponent.inject(this)
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
                                lockImpl.completeWakefulIntent(intent)
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

                    lockImpl.completeWakefulIntent(intent)
                    OTApp.logger.writeSystemLog("Released wake lock for AlarmReceiver.", TAG)
                    stopSelf(startId)
                }
            }

            return START_NOT_STICKY
        }
    }
}