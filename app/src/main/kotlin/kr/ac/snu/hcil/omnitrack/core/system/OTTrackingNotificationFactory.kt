package kr.ac.snu.hcil.omnitrack.core.system

import android.app.PendingIntent
import android.content.Context
import android.support.v4.app.NotificationCompat
import android.support.v4.app.TaskStackBuilder
import android.support.v4.content.ContextCompat
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.services.OTItemLoggingService
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemDetailActivity
import kr.ac.snu.hcil.omnitrack.utils.TextHelper

/**
 * Created by Young-Ho Kim on 16. 9. 1
 */
object OTTrackingNotificationFactory {

    private val TAG = "TrackingReminder"

    fun makeLoggingSuccessNotificationBuilder(context: Context, configuredContext: ConfiguredContext, trackerId: String, trackerName: String, itemId: String, loggedTime: Long, table: List<Pair<String, CharSequence?>>?, notificationId: Int, tag: String): NotificationCompat.Builder {
        val stackBuilder = TaskStackBuilder.create(context)
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(ItemDetailActivity::class.java)
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(
                ItemDetailActivity.makeItemEditPageIntent(itemId, trackerId, context)
                        .putExtra(OTApp.INTENT_EXTRA_NOTIFICATION_ID, notificationId)
                        .putExtra(OTApp.INTENT_EXTRA_NOTIFICATON_TAG, tag)
        )

        val resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val itemRemoveIntent = OTItemLoggingService.makeRemoveItemIntent(context, itemId)
                .putExtra(OTApp.INTENT_EXTRA_NOTIFICATION_ID, notificationId)
                .putExtra(OTApp.INTENT_EXTRA_NOTIFICATON_TAG, tag)

        val discardAction = NotificationCompat.Action.Builder(0,
                context.getString(R.string.msg_notification_action_discard_item),
                PendingIntent.getService(context, notificationId, itemRemoveIntent, PendingIntent.FLAG_UPDATE_CURRENT)).build()

        val editAction = NotificationCompat.Action.Builder(0, context.getString(R.string.msg_edit), resultPendingIntent).build()

        return makeBaseBuilder(context, loggedTime, OTNotificationManager.CHANNEL_ID_SYSTEM)
                .setSmallIcon(R.drawable.icon_simple_plus)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)
                .setContentTitle(String.format(context.getString(R.string.msg_notification_title_format_new_item),
                        trackerName
                ))
                .addAction(editAction)
                .addAction(discardAction)
                .apply {
                    if (table != null && table.isNotEmpty()) {
                        val inboxStyle = NotificationCompat.InboxStyle()
                        fun makeRowCharSequence(key: String, value: CharSequence?): CharSequence {
                            return if (value != null) {
                                TextHelper.fromHtml("<b>${key}</b> : ${value}")
                            } else {
                                TextHelper.fromHtml("<b>${key}</b> : [${context.getString(R.string.msg_empty_value)}]")
                            }
                        }

                        table.forEach { (key, value) ->
                            inboxStyle.addLine(makeRowCharSequence(key, value))
                        }
                        inboxStyle.setBigContentTitle(String.format(context.getString(R.string.msg_notification_format_inbox_title), trackerName))
                        setStyle(inboxStyle)
                        setContentText("${makeRowCharSequence(table.first().first, table.first().second)}...")
                    } else {
                        setContentText(context.getString(R.string.msg_notification_content_new_item))
                    }
                }
    }

    fun makeBaseBuilder(context: Context, time: Long, channelId: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channelId)
                .setWhen(time)
                .setShowWhen(true)
                .setSmallIcon(R.drawable.icon_simple)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
    }

}