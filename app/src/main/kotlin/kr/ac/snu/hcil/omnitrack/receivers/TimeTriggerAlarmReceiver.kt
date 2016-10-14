package kr.ac.snu.hcil.omnitrack.receivers

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.support.v4.content.WakefulBroadcastReceiver
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.database.LoggingDbHelper
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTimeTriggerAlarmManager
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-10-11.
 */
class TimeTriggerAlarmReceiver : WakefulBroadcastReceiver() {
    companion object {
        const val TAG = "TimeTriggerAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == OTApplication.BROADCAST_ACTION_TIME_TRIGGER_ALARM) {
            println("time trigger alarm")
            val serviceIntent = Intent(context, TimeTriggerWakefulHandlingService::class.java)
            serviceIntent.putExtras(intent)
            OTApplication.logger.writeSystemLog("Start wakeful service", TAG)
            startWakefulService(context, serviceIntent)
        }
    }

    class TimeTriggerWakefulHandlingService : IntentService("TimeTriggerHandlingService") {

        override fun onHandleIntent(intent: Intent) {

            val alarmId = intent.getIntExtra(OTTimeTriggerAlarmManager.INTENT_EXTRA_ALARM_ID, -1)
            val triggerTime = intent.getLongExtra(OTTimeTriggerAlarmManager.INTENT_EXTRA_TRIGGER_TIME, System.currentTimeMillis())

            OTApplication.logger.writeSystemLog("Wakeful Service handleIntent, trigger time: ${LoggingDbHelper.TIMESTAMP_FORMAT.format(Date(triggerTime))}", TAG)

            val triggers = OTApplication.app.timeTriggerAlarmManager.notifyAlarmFiredAndGetTriggers(alarmId, triggerTime, System.currentTimeMillis())


            if (triggers != null) {

                OTApplication.logger.writeSystemLog("${triggers.size} triggers will be fired.", TAG)

                var left = triggers.size
                if (left == 0) {
                    WakefulBroadcastReceiver.completeWakefulIntent(intent)
                } else {
                    for (trigger in triggers.withIndex()) {
                        trigger.value.fire(triggerTime) {
                            println("${trigger.index}-th trigger fire finished.")
                            OTApplication.logger.writeSystemLog("${trigger.index}-th trigger fire finished.", TAG)
                            left--
                        }
                    }

                    while (left > 0) {

                    }

                    OTApplication.logger.writeSystemLog("Every trigger firing was done. Release the wake lock.", TAG)

                    println("every trigger was done. finish the wakeup")
                    WakefulBroadcastReceiver.completeWakefulIntent(intent)

                }
            } else {
                WakefulBroadcastReceiver.completeWakefulIntent(intent)

                OTApplication.logger.writeSystemLog("No trigger is assigned to this alarm. Release the wake lock.", TAG)
            }
        }

    }
}