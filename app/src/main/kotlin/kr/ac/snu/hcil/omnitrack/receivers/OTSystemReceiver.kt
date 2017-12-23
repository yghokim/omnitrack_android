package kr.ac.snu.hcil.omnitrack.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApp

/**
 * Created by Young-Ho Kim on 16. 7. 28
 */
class OTSystemReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        println("OmniTrack system receiver received Intent: - ${intent.action}")
        when (intent.action) {

            OTApp.BROADCAST_ACTION_SHORTCUT_REFRESH -> {
                //OTShortcutPanelManager.refreshNotificationShortcutViews(user, context)
            }

            OTApp.BROADCAST_ACTION_ITEM_ADDED -> {
                /*
                OTApp.instance.currentUserObservable.subscribe {
                    user ->
                    val tracker = user[intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER)]
                    if (tracker != null) {
                        Toast.makeText(OTApp.instance, "${tracker.name} item was logged", Toast.LENGTH_SHORT).show()
                    }
                }

                context.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(context))
                */
            }

            OTApp.BROADCAST_ACTION_ITEM_REMOVED -> {
            }

            OTApp.BROADCAST_ACTION_COMMAND_REMOVE_ITEM -> {
                /*
                OTApp.instance.currentUserObservable.subscribe {
                    user ->
                    val tracker = user[intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER)]
                    if (tracker != null) {
                        val notificationId = intent.getIntExtra(OTApp.INTENT_EXTRA_NOTIFICATION_ID_SEED, 1)
                        val itemId = intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM)
                        if (itemId != null) {
                            //TODO item remove
                            //OTApp.instance.databaseManager.removeItem(tracker.objectId, itemId)
                        }

                        OTTrackingNotificationFactory.cancelBackgroundLoggingSuccessNotification(tracker, notificationId)
                    }
                }

                context.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(context))
                */
            }

            OTApp.BROADCAST_ACTION_BACKGROUND_LOGGING_STARTED -> {
                /*
                OTApp.instance.currentUserObservable.subscribe {
                    user ->
                    val tracker = user[intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER)]
                    if (tracker != null) {
                        val notificationId = intent.getIntExtra(OTApp.INTENT_EXTRA_NOTIFICATION_ID_SEED, 1)
                        OTTaskNotificationManager.setTaskProgressNotification(context, tracker.objectId, notificationId, String.format(context.getString(R.string.msg_background_logging_notification_title_format), tracker.name), context.getString(R.string.msg_background_logging_started_notification_message), OTTaskNotificationManager.PROGRESS_INDETERMINATE,
                                R.drawable.icon_cloud_upload, R.drawable.icon_cloud_upload)
                    }
                }*/
            }

            OTApp.BROADCAST_ACTION_BACKGROUND_LOGGING_SUCCEEDED -> {
                /*
                println("background logging successful")
                OTApp.instance.currentUserObservable.subscribe {
                    user ->
                    val tracker = user[intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER)]
                    if (tracker != null) {
                        val itemId = intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM)
                        val notify = intent.getBooleanExtra(OTBackgroundLoggingService.INTENT_EXTRA_NOTIFY, true)
                        if (itemId != null && notify) {
                            val notificationId = intent.getIntExtra(OTApp.INTENT_EXTRA_NOTIFICATION_ID_SEED, 1)
                            OTTaskNotificationManager.dismissNotification(context, notificationId, tracker.objectId)
                            OTTrackingNotificationFactory.pushBackgroundLoggingSuccessNotification(context, tracker, itemId, System.currentTimeMillis(), notificationId)
                        }
                    }
                }*/
            }

            OTApp.BROADCAST_ACTION_EVENT_TRIGGER_CHECK_ALARM -> {
                //OTDataTriggerManager.checkMeasures(context)
            }
        }
    }
}