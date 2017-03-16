package kr.ac.snu.hcil.omnitrack.core.system

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.TaskStackBuilder
import android.support.v4.content.ContextCompat
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemBrowserActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemEditingActivity
import kr.ac.snu.hcil.omnitrack.utils.FillingIntegerIdReservationTable
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by Young-Ho Kim on 16. 9. 1
 */
object OTTrackingNotificationManager {

    private val increment = AtomicInteger(500)

    private val reminderTrackerPendingCounts = Hashtable<String, Int>()

    private val reminderTrackerNotificationIdTable = FillingIntegerIdReservationTable<String>()
    private val backgroundLoggingTrackerNotificationIdTable = FillingIntegerIdReservationTable<String>()

    /*
    private val icon_ex: Icon by lazy{
        Icon.createWithBitmap(BitmapFactory.decodeResource(OTApplication.app.resources, R.drawable.ex_dark))
    }*/

    private fun getNewReminderNotificationId(tracker: OTTracker): Int {
        return reminderTrackerNotificationIdTable[tracker.objectId]
    }

    private fun getNewBackgroundLoggingNotificationId(tracker: OTTracker): Int {
        return 500 + backgroundLoggingTrackerNotificationIdTable[tracker.objectId]
    }

    enum class Type(val priority: Int, val collapse: Boolean) {
        TRACKING_REMINDER(Notification.PRIORITY_MAX, false),
        BACKGROUND_LOGGING_NOTIFICATION(Notification.PRIORITY_DEFAULT, true),
        BACKGROUND_LOGGING_PROGRESS(Notification.PRIORITY_DEFAULT, false);


        fun getNewId(): Int {
            return if (collapse) {
                values().indexOf(this)
            } else {
                increment.incrementAndGet()
            }
        }
    }

    private val notificationService: NotificationManager by lazy {
        OTApplication.app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun pushReminderNotification(context: Context, tracker: OTTracker, reminderTime: Long) {
        val stackBuilder = TaskStackBuilder.create(context)
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(ItemEditingActivity::class.java)
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(ItemEditingActivity.makeIntent(tracker.objectId, reminderTime, context))
        val resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = makeBaseBuilder(context, Type.TRACKING_REMINDER.priority, reminderTime)
                .setContentIntent(resultPendingIntent)
                .setContentText(String.format(context.resources.getString(R.string.msg_noti_tap_for_tracking_format), tracker.name))
                .setDefaults(Notification.DEFAULT_ALL)

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


        notificationService.notify(getNewReminderNotificationId(tracker), builder.build())
    }

    fun cancelBackgroundLoggingSuccessNotification(tracker: OTTracker) {

        notificationService.cancel(getNewBackgroundLoggingNotificationId(tracker))
    }

    fun pushBackgroundLoggingSuccessNotification(context: Context, tracker: OTTracker, itemId: String, loggedTime: Long) {
        val stackBuilder = TaskStackBuilder.create(context)
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(ItemBrowserActivity::class.java)
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(ItemBrowserActivity.makeIntent(tracker, context))

        val resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val itemRemoveIntent = Intent(OTApplication.BROADCAST_ACTION_COMMAND_REMOVE_ITEM)
        itemRemoveIntent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
        itemRemoveIntent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM, itemId)

        val discardAction = NotificationCompat.Action.Builder(R.drawable.ex,
                context.resources.getString(R.string.msg_notification_action_discard_item),
                PendingIntent.getBroadcast(context, 0, itemRemoveIntent, PendingIntent.FLAG_UPDATE_CURRENT)).build()

        val builder = makeBaseBuilder(context, Type.BACKGROUND_LOGGING_NOTIFICATION.priority, loggedTime)
                .setSmallIcon(R.drawable.icon_simple_plus)
                .setContentIntent(resultPendingIntent)
                .setContentText(
                        String.format(OTApplication.app.resources.getString(R.string.msg_notification_content_format_new_item),
                                tracker.name
                        ))
                .addAction(discardAction)

        notificationService.notify(getNewBackgroundLoggingNotificationId(tracker), builder.build())
    }

    fun notifyReminderChecked(trackerId: String, reminderTime: Long) {
        println("reminder checked - ${reminderTime}")
        //if(reminderTrackerPendingCounts[trackerId] != null)
        //{
        //  if(reminderTrackerPendingCounts[trackerId]!! == 1)
        //  {
        //remove reminder
        reminderTrackerPendingCounts.remove(trackerId)
        notificationService.cancel(reminderTrackerNotificationIdTable[trackerId])
        reminderTrackerNotificationIdTable.removeKey(trackerId)
        //  }
        //  else{
        //      reminderTrackerPendingCounts[trackerId] = reminderTrackerPendingCounts[trackerId]!! - 1
        //  }

        // }
    }

    fun pushWarningMessageNotification(context: Context, message: String) {

    }

    fun makeBaseBuilder(context: Context, priority: Int, time: Long): NotificationCompat.Builder {

        val builder = NotificationCompat.Builder(context).setPriority(priority)
                .setWhen(time)
                .setShowWhen(true)
                .setSmallIcon(R.drawable.icon_simple)

        if (Build.VERSION.SDK_INT >= 21) {
            builder.setColor(ContextCompat.getColor(context, R.color.colorPrimary))
        }


        return builder
    }

}