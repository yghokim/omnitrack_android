package kr.ac.snu.hcil.omnitrack.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTShortcutManager
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTimeTriggerAlarmManager

/**
 * Created by younghokim on 16. 7. 28..
 */
class OTSystemReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        println("OmniTrack system receiver received Intent: - ${intent.action}")
        when (intent.action) {
            OTApplication.BROADCAST_ACTION_ALARM -> {
                //triggerId: String, UserId: String, alarmId: Int
                //val userId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_USER)
                val alarmId = intent.getIntExtra(OTTimeTriggerAlarmManager.INTENT_EXTRA_ALARM_ID, -1)
                val triggerTime = intent.getLongExtra(OTTimeTriggerAlarmManager.INTENT_EXTRA_TRIGGER_TIME, System.currentTimeMillis())

                OTTimeTriggerAlarmManager.notifyAlarmFired(alarmId, triggerTime, System.currentTimeMillis())

            }

            OTApplication.BROADCAST_ACTION_SHORTCUT_REFRESH->{
                OTShortcutManager.refreshNotificationShortcutViews(context)
            }

        }
    }

}