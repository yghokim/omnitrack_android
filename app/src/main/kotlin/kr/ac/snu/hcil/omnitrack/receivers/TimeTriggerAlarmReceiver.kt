package kr.ac.snu.hcil.omnitrack.receivers

import android.app.IntentService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.SparseArray
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.LoggingDbHelper
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTimeTriggerAlarmManager
import java.util.*

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
        if (intent.action == OTApp.BROADCAST_ACTION_TIME_TRIGGER_ALARM) {
            println("time trigger alarm")
            val serviceIntent = Intent(context, TimeTriggerWakefulHandlingService::class.java)
            serviceIntent.putExtras(intent)
            OTApp.logger.writeSystemLog("Start wakeful service", TAG)
            startWakefulService(context, serviceIntent)
        }
    }

    class TimeTriggerWakefulHandlingService : IntentService("TimeTriggerHandlingService") {

        override fun onHandleIntent(intent: Intent) {
            val alarmId = intent.getIntExtra(OTTimeTriggerAlarmManager.INTENT_EXTRA_ALARM_ID, -1)
            val triggerTime = intent.getLongExtra(OTTimeTriggerAlarmManager.INTENT_EXTRA_TRIGGER_TIME, System.currentTimeMillis())


            println("Wakeful Service handleIntent, trigger time: ${LoggingDbHelper.TIMESTAMP_FORMAT.format(Date(triggerTime))}")

            /*TODO trigger alarm service handling
            OTApp.instance.currentUserObservable.first().flatMap {
                user ->
                println("Returned user")
                val triggerSchedules = OTApp.instance.timeTriggerAlarmManager.handleAlarmAndGetTriggerInfo(user, alarmId, triggerTime, System.currentTimeMillis())
                println("Handled system alarm and retrieved corresponding trigger schedules - ${triggerSchedules?.size}")

                OTApp.logger.writeSystemLog("Handled system alarm and retrieved corresponding trigger schedules: ${triggerSchedules?.size}", TAG)
                if (triggerSchedules != null) {

                    OTApp.logger.writeSystemLog("${triggerSchedules.size} triggers will be fired.", TAG)

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
                        OTApp.logger.writeSystemLog("Every trigger firing was done. Release the wake lock.", TAG)

                        println("every trigger was done. finish the wakeup")

                        OTNotificationTriggerAction.notifyPopupQueue(this)
                    }
                } else {
                    Observable.just(null)
                }
            }.subscribe({

            }, { }, { completeWakefulIntent(intent) })*/
        }
    }
}