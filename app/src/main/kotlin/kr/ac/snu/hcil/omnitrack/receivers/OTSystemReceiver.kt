package kr.ac.snu.hcil.omnitrack.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApplication

/**
 * Created by younghokim on 16. 7. 28..
 */
class OTSystemReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        println("OmniTrack system receiver received Intent: - ${intent.action}")
        when (intent.action) {
            OTApplication.BROADCAST_ACTION_ALARM -> {
                //triggerId: String, UserId: String, alarmId: Int
                val triggerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRIGGER)
                //val userId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_USER)

                println("alarm of ${OTApplication.app.triggerManager.getTriggerWithId(triggerId)?.name}")
                OTApplication.app.triggerManager.getTriggerWithId(triggerId)?.fire()
            }
        }
    }

}