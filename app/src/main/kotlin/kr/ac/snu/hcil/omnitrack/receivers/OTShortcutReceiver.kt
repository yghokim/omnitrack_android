package kr.ac.snu.hcil.omnitrack.receivers

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.support.v4.app.TaskStackBuilder
import android.support.v4.graphics.ColorUtils
import android.support.v7.app.NotificationCompat
import android.view.View
import android.widget.RemoteViews
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.services.OTBackgroundLoggingService
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemEditingActivity

class OTShortcutReceiver : BroadcastReceiver() {

    companion object{
        const val NOTIFICATION_ID = 200000

        const val MAX_NUM_SHORTCUTS = 5
    }

    private fun getTracker(intent: Intent): OTTracker?
    {
        val id = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
        return OTApplication.app.currentUser[id]
    }

    override fun onReceive(context: Context, intent: Intent) {

        // an Intent broadcast.
        when(intent.action)
        {
            OTApplication.BROADCAST_ACTION_SHORTCUT_EXCLUDE_TRACKER->
            {
                val tracker = getTracker(intent)
                if(tracker!=null)
                {
                    println("exclude tracker ${tracker.name} from shortcut")
                    refreshNotificationShortcutViews(context)
                }
            }

            OTApplication.BROADCAST_ACTION_SHORTCUT_INCLUDE_TRACKER->
            {
                val tracker = getTracker(intent)
                if(tracker!=null)
                {
                    println("include tracker ${tracker.name} to shortcut")
                    refreshNotificationShortcutViews(context)
                }
            }

            OTApplication.BROADCAST_ACTION_SHORTCUT_OPEN_TRACKER->
            {

            }

            OTApplication.BROADCAST_ACTION_SHORTCUT_PUSH_NOW->
            {

            }

            OTApplication.BROADCAST_ACTION_SHORTCUT_TRACKER_INFO_CHANGED -> {
                refreshNotificationShortcutViews(context)
            }
        }
    }

    fun buildNewNotificationShortcutViews(context: Context, bigStyle: Boolean): RemoteViews
    {
        val rv = RemoteViews(context.packageName, if (bigStyle) R.layout.remoteview_shortcut_notification_big else R.layout.remoteview_shortcut_notification_normal)

        if(bigStyle)
        {
            //header exist.

            val stackBuilder = TaskStackBuilder.create(context)
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(HomeActivity::class.java)
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(Intent(context, HomeActivity::class.java))

            val morePendingIntent = stackBuilder.getPendingIntent(0,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            rv.setOnClickPendingIntent(R.id.ui_button_more, morePendingIntent)
        }

        val trackers = OTApplication.app.currentUser.getTrackersOnShortcut()

        rv.removeAllViews(R.id.container)

        for(i in 0..MAX_NUM_SHORTCUTS-1)
        {
            val element = RemoteViews(context.packageName, if (bigStyle) R.layout.remoteview_shortcut_notification_element else R.layout.remoteview_shortcut_notification_element_normal)

            if(trackers.size-1 < i)
            {
                element.setViewVisibility(R.id.ui_button_instant, View.INVISIBLE)
                element.setViewVisibility(R.id.ui_name, View.INVISIBLE)
            }
            else{
                element.setViewVisibility(R.id.ui_button_instant, View.VISIBLE)
                element.setViewVisibility(R.id.ui_name, View.VISIBLE)

                element.setInt(R.id.ui_button_container, "setBackgroundColor", ColorUtils.setAlphaComponent(trackers[i].color, 200))

                element.setTextViewText(R.id.ui_name, trackers[i].name)


                val instantLoggingIntent = PendingIntent.getService(context, i, OTBackgroundLoggingService.makeIntent(context, trackers[i]), PendingIntent.FLAG_UPDATE_CURRENT)
                val openItemActivityIntent = PendingIntent.getActivity(context, i, ItemEditingActivity.makeIntent(trackers[i].objectId, context), PendingIntent.FLAG_UPDATE_CURRENT)

                element.setOnClickPendingIntent(R.id.ui_button_instant, instantLoggingIntent)
                element.setOnClickPendingIntent(R.id.group, openItemActivityIntent)
            }

            rv.addView(R.id.container, element)
        }

        return rv
    }

    fun refreshNotificationShortcutViews(context: Context) {
        val bigView = buildNewNotificationShortcutViews(context, true)
        val normalView = buildNewNotificationShortcutViews(context, false)

        val noti = NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.icon_simple_white)
            .setContentTitle(context.resources.getString(R.string.app_name))
            .setCustomBigContentView(bigView)
            .setCustomContentView(normalView)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setStyle(android.support.v4.app.NotificationCompat.BigTextStyle())
            .setPriority(Notification.PRIORITY_MAX)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, noti)
    }
}
