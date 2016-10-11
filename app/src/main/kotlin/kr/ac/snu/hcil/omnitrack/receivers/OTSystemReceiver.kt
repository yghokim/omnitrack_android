package kr.ac.snu.hcil.omnitrack.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.database.DatabaseHelper
import kr.ac.snu.hcil.omnitrack.core.system.OTNotificationManager
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTDataTriggerManager

/**
 * Created by Young-Ho Kim on 16. 7. 28
 */
class OTSystemReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        println("OmniTrack system receiver received Intent: - ${intent.action}")
        when (intent.action) {

            OTApplication.BROADCAST_ACTION_SHORTCUT_REFRESH->{
                OTShortcutManager.refreshNotificationShortcutViews(context)
            }

            OTApplication.BROADCAST_ACTION_ITEM_ADDED -> {
                val tracker = OTApplication.app.currentUser[intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)]
                if (tracker != null) {
                    Toast.makeText(OTApplication.app, "${tracker.name} item was logged", Toast.LENGTH_SHORT).show()
                }
            }

            OTApplication.BROADCAST_ACTION_COMMAND_REMOVE_ITEM -> {
                val tracker = OTApplication.app.currentUser[intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)]
                if (tracker != null) {
                    val itemDbId = intent.getLongExtra(OTApplication.INTENT_EXTRA_DB_ID_ITEM, -1)
                    if (itemDbId != -1L) {
                        OTApplication.app.dbHelper.deleteObjects(DatabaseHelper.ItemScheme, itemDbId)
                    }

                    OTNotificationManager.cancelBackgroundLoggingSuccessNotification(tracker)
                }
            }

            OTApplication.BROADCAST_ACTION_BACKGROUND_LOGGING_STARTED -> {

            }

            OTApplication.BROADCAST_ACTION_BACKGROUND_LOGGING_SUCCEEDED -> {
                println("background logging successful")
                val tracker = OTApplication.app.currentUser[intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)]
                if (tracker != null) {
                    val itemDbId = intent.getLongExtra(OTApplication.INTENT_EXTRA_DB_ID_ITEM, -1)
                    if (itemDbId != -1L) {
                        OTNotificationManager.pushBackgroundLoggingSuccessNotification(context, tracker, itemDbId, System.currentTimeMillis())
                    }
                }
            }

            OTApplication.BROADCAST_ACTION_EVENT_TRIGGER_CHECK_ALARM->{
                OTDataTriggerManager.checkMeasures(context)
            }
        }
    }

}