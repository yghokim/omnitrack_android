package kr.ac.snu.hcil.omnitrack.receivers

import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.SparseArray
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.triggers.ITriggerAlarmController
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
        when (intent.action) {
            OTApp.BROADCAST_ACTION_TIME_TRIGGER_ALARM -> {
                println("time trigger alarm")
                val serviceIntent = Intent(context, TimeTriggerWakefulHandlingService::class.java)
                serviceIntent.putExtras(intent)
                OTApp.logger.writeSystemLog("Start wakeful service", TAG)
                startWakefulService(context, serviceIntent)
            }
            OTApp.BROADCAST_ACTION_REMINDER_EXPIRY_ALARM -> {
                println("reminder auto durationSeconds alarm")
                OTApp.logger.writeSystemLog("Received reminder auto expiry alarm", TAG)
                val dismissIntent = Intent(context, OTReminderService::class.java).apply {
                    this.action = OTReminderService.ACTION_ON_USER_DISMISS
                    this.putExtras(intent)
                }
                context.startService(dismissIntent)
            }
        }
    }

    class TimeTriggerWakefulHandlingService : Service() {
        override fun onBind(p0: Intent?): IBinder {
            TODO()
        }

        @Inject
        protected lateinit var triggerAlarmController: ITriggerAlarmController

        override fun onCreate() {
            super.onCreate()
            (application as OTApp).currentConfiguredContext.triggerSystemComponent.inject(this)
        }

        override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

            val alarmId = intent.getIntExtra(OTTriggerAlarmManager.INTENT_EXTRA_ALARM_ID, -1)

            OTApp.logger.writeSystemLog("Wakeful Trigger Alarm Service handleIntent", TAG)

            triggerAlarmController.onAlarmFired(alarmId).doAfterTerminate {
                completeWakefulIntent(intent)
                OTApp.logger.writeSystemLog("Released wake lock for AlarmReceiver.", TAG)
                stopSelf(startId)
            }.subscribe({
                println("successfully handled fired alarm: ${alarmId}")
                OTApp.logger.writeSystemLog("successfully handled fired alarm: ${alarmId}", TAG)
            }, { err ->
                println("trigger alarm handling error")
                err.printStackTrace()
            })

            return START_NOT_STICKY
        }

        fun onHandleIntent(intent: Intent) {
            val alarmId = intent.getIntExtra(OTTriggerAlarmManager.INTENT_EXTRA_ALARM_ID, -1)

            OTApp.logger.writeSystemLog("Wakeful Trigger Alarm Service handleIntent", TAG)

            triggerAlarmController.onAlarmFired(alarmId).doAfterTerminate {
            }.subscribe({
                println("successfully handled fired alarm: ${alarmId}")
                OTApp.logger.writeSystemLog("successfully handled fired alarm: ${alarmId}", TAG)
                completeWakefulIntent(intent)
                println("relaesed wake lock for AlarmReceiver.")
            }, { err ->
                println("trigger alarm handling error")
                err.printStackTrace()
                completeWakefulIntent(intent)
                println("relaesed wake lock for AlarmReceiver.")
            })


            /*TODO trigger alarm service handling
            OTApp.instance.currentUserObservable.first().flatMap {
                user ->
                println("Returned user")
                val triggerSchedules = OTApp.instance.triggerAlarmManager.handleFiredAlarmAndGetTriggerInfo(user, alarmId, triggerTime, System.currentTimeMillis())
                println("Handled system alarm and retrieved corresponding trigger schedules - ${triggerSchedules?.size}")

                OTApp.eventLogger.writeSystemLog("Handled system alarm and retrieved corresponding trigger schedules: ${triggerSchedules?.size}", TAG)
                if (triggerSchedules != null) {

                    OTApp.eventLogger.writeSystemLog("${triggerSchedules.size} triggers will be fired.", TAG)

                    Observable.merge(triggerSchedules.map {
                        schedule ->
                        user.getTriggerObservable(schedule.triggerId).onErrorReturn { err -> null }.flatMap {
                            trigger: OTTrigger? ->
                            trigger?.fire(triggerTime, this) ?: Observable.just(null)
                        }.doOnNext {
                            trigger: OTTrigger? ->
                            if (trigger != null) {
                                if (schedule.oneShot) {
                                    trigger.isOn = false
                                } else {
                                    (trigger as OTTimeTrigger).reserveNextAlarmToSystem(triggerTime)
                                }
                            }
                        }
                    }).observeOn(Schedulers.immediate()).doOnCompleted {
                        OTApp.eventLogger.writeSystemLog("Every trigger firing was done. Release the wake lock.", TAG)

                        println("every trigger was done. finish the wakeup")

                        OTReminderAction.notifyPopupQueue(this)
                    }
                } else {
                    Observable.just(null)
                }
            }.subscribe({

            }, { }, { completeWakefulIntent(intent) })*/
        }
    }
}