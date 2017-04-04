package kr.ac.snu.hcil.omnitrack.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseDbHelper
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.core.system.OTTaskNotificationManager
import kr.ac.snu.hcil.omnitrack.core.system.OTTrackingNotificationManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTDataTriggerManager
import kr.ac.snu.hcil.omnitrack.services.OTBackgroundLoggingService
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetUpdateService

/**
 * Created by Young-Ho Kim on 16. 7. 28
 */
class OTSystemReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        println("OmniTrack system receiver received Intent: - ${intent.action}")
        when (intent.action) {

            OTApplication.BROADCAST_ACTION_SHORTCUT_REFRESH -> {
                OTApplication.app.currentUserObservable.subscribe {
                    user ->
                    OTShortcutPanelManager.refreshNotificationShortcutViews(user, context)
                }
            }

            OTApplication.BROADCAST_ACTION_ITEM_ADDED -> {
                OTApplication.app.currentUserObservable.subscribe {
                    user ->
                    val tracker = user[intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)]
                    if (tracker != null) {
                        Toast.makeText(OTApplication.app, "${tracker.name} item was logged", Toast.LENGTH_SHORT).show()
                        }
                }

                context.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(context))
            }

            OTApplication.BROADCAST_ACTION_ITEM_REMOVED -> {
                context.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(context))
            }

            OTApplication.BROADCAST_ACTION_COMMAND_REMOVE_ITEM -> {
                OTApplication.app.currentUserObservable.subscribe {
                    user ->
                    val tracker = user[intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)]
                    if (tracker != null) {
                        val notificationId = intent.getIntExtra(OTApplication.INTENT_EXTRA_NOTIFICATION_ID_SEED, 1)
                        val itemId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM)
                        if (itemId != null) {
                            FirebaseDbHelper.removeItem(tracker.objectId, itemId)
                            }

                        OTTrackingNotificationManager.cancelBackgroundLoggingSuccessNotification(tracker, notificationId)
                        }
                }

                context.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(context))
            }

            OTApplication.BROADCAST_ACTION_BACKGROUND_LOGGING_STARTED -> {
                OTApplication.app.currentUserObservable.subscribe {
                    user ->
                    val tracker = user[intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)]
                    if (tracker != null) {
                        val notificationId = intent.getIntExtra(OTApplication.INTENT_EXTRA_NOTIFICATION_ID_SEED, 1)
                        OTTaskNotificationManager.setTaskProgressNotification(context, tracker.objectId, notificationId, String.format(context.getString(R.string.msg_background_logging_notification_title_format), tracker.name), context.getString(R.string.msg_background_logging_started_notification_message), OTTaskNotificationManager.PROGRESS_INDETERMINATE,
                                R.drawable.icon_cloud_upload, R.drawable.icon_cloud_upload)
                        }
                }
            }

            OTApplication.BROADCAST_ACTION_BACKGROUND_LOGGING_SUCCEEDED -> {
                println("background logging successful")
                OTApplication.app.currentUserObservable.subscribe {
                    user ->
                    val tracker = user[intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)]
                    if (tracker != null) {
                        val itemId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM)
                        val notify = intent.getBooleanExtra(OTBackgroundLoggingService.INTENT_EXTRA_NOTIFY, true)
                        if (itemId != null && notify) {
                            val notificationId = intent.getIntExtra(OTApplication.INTENT_EXTRA_NOTIFICATION_ID_SEED, 1)
                            OTTaskNotificationManager.dismissNotification(context, notificationId, tracker.objectId)
                            OTTrackingNotificationManager.pushBackgroundLoggingSuccessNotification(context, tracker, itemId, System.currentTimeMillis(), notificationId)
                            }
                        }
                    }
                }

            OTApplication.BROADCAST_ACTION_EVENT_TRIGGER_CHECK_ALARM -> {
                OTDataTriggerManager.checkMeasures(context)
            }
        }
    }
}