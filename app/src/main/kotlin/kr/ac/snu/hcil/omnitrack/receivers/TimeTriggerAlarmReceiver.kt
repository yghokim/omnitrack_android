package kr.ac.snu.hcil.omnitrack.receivers

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.support.v4.content.WakefulBroadcastReceiver
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTimeTriggerAlarmManager

/**
 * Created by Young-Ho Kim on 2016-10-11.
 */
class TimeTriggerAlarmReceiver : WakefulBroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == OTApplication.BROADCAST_ACTION_TIME_TRIGGER_ALARM) {
            println("time trigger alarm")
            val serviceIntent = Intent(context, TimeTriggerWakefulHandlingService::class.java)
            serviceIntent.putExtras(intent)

            startWakefulService(context, serviceIntent)
        }
    }

    class TimeTriggerWakefulHandlingService : IntentService("TimeTriggerHandlingService") {

        override fun onHandleIntent(intent: Intent) {

            val alarmId = intent.getIntExtra(OTTimeTriggerAlarmManager.INTENT_EXTRA_ALARM_ID, -1)
            val triggerTime = intent.getLongExtra(OTTimeTriggerAlarmManager.INTENT_EXTRA_TRIGGER_TIME, System.currentTimeMillis())

            val triggers = OTTimeTriggerAlarmManager.notifyAlarmFiredAndGetTriggers(alarmId, triggerTime, System.currentTimeMillis())

            if (triggers != null) {
                var left = triggers.size
                if (left == 0) {
                    WakefulBroadcastReceiver.completeWakefulIntent(intent)
                } else {
                    for (trigger in triggers) {
                        trigger.fire(triggerTime) {
                            println("${trigger.action} fire finished.")
                            left--
                        }
                    }

                    while (left > 0) {
                        1
                    }

                    println("every trigger was done. finish the wakeup")
                    WakefulBroadcastReceiver.completeWakefulIntent(intent)

                }
            } else {
                WakefulBroadcastReceiver.completeWakefulIntent(intent)
            }
        }

    }
}