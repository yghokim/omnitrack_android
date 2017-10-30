package kr.ac.snu.hcil.omnitrack.core.system

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.support.v4.app.TaskStackBuilder
import android.support.v4.content.ContextCompat
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.services.OTItemLoggingService
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemDetailActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.settings.SettingsActivity
import kr.ac.snu.hcil.omnitrack.utils.FillingIntegerIdReservationTable
import kr.ac.snu.hcil.omnitrack.utils.TextHelper
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by Young-Ho Kim on 16. 9. 1
 */
object OTTrackingNotificationFactory {

    private val TAG = "TrackingReminder"
    private val increment = AtomicInteger(500)

    private val reminderTrackerPendingCounts = Hashtable<String, Int>()

    private val reminderTrackerNotificationIdTable = FillingIntegerIdReservationTable<String>()

    private fun getNewReminderNotificationId(tracker: OTTracker): Int {
        return reminderTrackerNotificationIdTable[tracker.objectId]
    }

    private val notificationService: NotificationManager by lazy {
        OTApp.instance.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun pushReminderNotification(context: Context, tracker: OTTracker, reminderTime: Long) {
        val stackBuilder = TaskStackBuilder.create(context)
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(ItemDetailActivity::class.java)
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(ItemDetailActivity.makeIntent(tracker.objectId, reminderTime, context))
        val resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val ringtone = PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsActivity.PREF_REMINDER_NOTI_RINGTONE, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.toString())
        val lightColor = PreferenceManager.getDefaultSharedPreferences(context).getInt(SettingsActivity.PREF_REMINDER_LIGHT_COLOR, ContextCompat.getColor(context, R.color.colorPrimary))

        val builder = makeBaseBuilder(context, reminderTime, OTNotificationManager.CHANNEL_ID_IMPORTANT)
                .setContentIntent(resultPendingIntent)
                .setContentText(String.format(context.resources.getString(R.string.msg_noti_tap_for_tracking_format), tracker.name))
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setSound(Uri.parse(
                        ringtone
                ))
                .setLights(lightColor, 1000, 500)

        if (reminderTrackerPendingCounts.containsKey(tracker.objectId)) {
            println("merge reminder - ${tracker.name}")
            //not first. merge notification
            reminderTrackerPendingCounts[tracker.objectId] = reminderTrackerPendingCounts[tracker.objectId]!! + 1

            builder.setAutoCancel(false)
                    .setContentTitle("${reminderTrackerPendingCounts[tracker.objectId]} ${tracker.name} Reminders")
        } else {
            println("show new reminder - ${tracker.name}")
            //first time. this is the only notification with that tracker.
            reminderTrackerPendingCounts[tracker.objectId] = 1

            builder.setAutoCancel(true)
                    .setContentTitle("${tracker.name} Reminder")
        }


        notificationService.notify(TAG, getNewReminderNotificationId(tracker), builder.build())
    }

    fun makeLoggingSuccessNotificationBuilder(context: Context, trackerId: String, trackerName: String, itemId: String, loggedTime: Long, table: List<Pair<String, CharSequence?>>?, notificationId: Int, tag: String): NotificationCompat.Builder {
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
                OTApp.getString(R.string.msg_notification_action_discard_item),
                PendingIntent.getService(context, notificationId, itemRemoveIntent, PendingIntent.FLAG_UPDATE_CURRENT)).build()

        val editAction = NotificationCompat.Action.Builder(0, OTApp.getString(R.string.msg_edit), resultPendingIntent).build()

        return makeBaseBuilder(context, loggedTime, OTNotificationManager.CHANNEL_ID_SYSTEM)
                .setSmallIcon(R.drawable.icon_simple_plus)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)
                .setContentTitle(String.format(OTApp.getString(R.string.msg_notification_title_format_new_item),
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
                                TextHelper.fromHtml("<b>${key}</b> : [${OTApp.getString(R.string.msg_empty_value)}]")
                            }
                        }

                        table.forEach { (key, value) ->
                            inboxStyle.addLine(makeRowCharSequence(key, value))
                        }
                        inboxStyle.setBigContentTitle(String.format(OTApp.getString(R.string.msg_notification_format_inbox_title), trackerName))
                        setStyle(inboxStyle)
                        setContentText("${makeRowCharSequence(table.first().first, table.first().second)}...")
                    } else {
                        setContentText(OTApp.getString(R.string.msg_notification_content_new_item))
                    }
                }
    }

    fun notifyReminderChecked(trackerId: String, reminderTime: Long) {
        println("reminder checked - ${reminderTime}")
        //if(reminderTrackerPendingCounts[trackerId] != null)
        //{
        //  if(reminderTrackerPendingCounts[trackerId]!! == 1)
        //  {
        //remove reminder
        reminderTrackerPendingCounts.remove(trackerId)
        notificationService.cancel(TAG, reminderTrackerNotificationIdTable[trackerId])
        reminderTrackerNotificationIdTable.removeKey(trackerId)
        //  }
        //  else{
        //      reminderTrackerPendingCounts[trackerId] = reminderTrackerPendingCounts[trackerId]!! - 1
        //  }

        // }
    }

    fun makeBaseBuilder(context: Context, time: Long, channelId: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channelId)
                .setWhen(time)
                .setShowWhen(true)
                .setSmallIcon(R.drawable.icon_simple)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
    }

}