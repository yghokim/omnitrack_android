package kr.ac.snu.hcil.omnitrack.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication

/**
 * Created by younghokim on 16. 7. 28..
 */
class OTSystemReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        println("OmniTrack system receiver received Intent: - ${intent.action}")
        when (intent.action) {
            OmniTrackApplication.BROADCAST_ACTION_ALARM -> {
                //triggerId: String, UserId: String, alarmId: Int
                val triggerId = intent.getStringExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_TRIGGER)
                val userId = intent.getStringExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_USER)

                println("alarm of ${OmniTrackApplication.app.triggerManager.getTriggerWithId(triggerId)?.name}")
                OmniTrackApplication.app.triggerManager.getTriggerWithId(triggerId)?.fire()
            }
        }
    }

}