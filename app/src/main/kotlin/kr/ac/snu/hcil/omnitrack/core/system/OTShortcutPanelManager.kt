package kr.ac.snu.hcil.omnitrack.core.system

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.preference.PreferenceManager
import android.support.v4.app.TaskStackBuilder
import android.support.v4.graphics.ColorUtils
import android.support.v7.app.NotificationCompat
import android.view.View
import android.widget.RemoteViews
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.services.OTBackgroundLoggingService
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemEditingActivity
import kr.ac.snu.hcil.omnitrack.utils.VectorIconHelper
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetUpdateService

/**
 * Created by Young-Ho Kim on 9/4/2016
 */
object OTShortcutPanelManager {

    const val NOTIFICATION_ID = 200000

    const val MAX_NUM_SHORTCUTS = 5

    val showPanels: Boolean get() {
        return PreferenceManager.getDefaultSharedPreferences(OTApplication.app).getBoolean("pref_show_shortcut_panel", false)
    }

    private val notificationManager: NotificationManager by lazy {
        (OTApplication.app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
    }

    private fun buildNewNotificationShortcutViews(user: OTUser, context: Context, bigStyle: Boolean): RemoteViews
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

        val trackers = user.getTrackersOnShortcut()

        rv.removeAllViews(R.id.container)

        val buttonSize = OTApplication.app.resourcesWrapped.getDimensionPixelSize(R.dimen.button_height_small)
        val buttonRadius = buttonSize * .5f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL

        for (i in 0..MAX_NUM_SHORTCUTS - 1)
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

                //

                element.setTextViewText(R.id.ui_name, trackers[i].name)

                if (true) {
                    val buttonBitmap = Bitmap.createBitmap(buttonSize, buttonSize, Bitmap.Config.ARGB_8888)
                    val buttonCanvas = Canvas(buttonBitmap)
                    paint.color = ColorUtils.setAlphaComponent(trackers[i].color, 200)
                    buttonCanvas.drawCircle(buttonRadius, buttonRadius, buttonRadius, paint)
                    element.setImageViewBitmap(R.id.ui_background_image, buttonBitmap)
                    element.setImageViewBitmap(R.id.ui_button_instant, VectorIconHelper.getConvertedBitmap(context, R.drawable.instant_add))
                } else {
                    element.setInt(R.id.ui_button_container, "setBackgroundColor", ColorUtils.setAlphaComponent(trackers[i].color, 200))
                    element.setViewVisibility(R.id.ui_background_image, View.INVISIBLE)
                }

                val instantLoggingIntent = PendingIntent.getService(context, i, OTBackgroundLoggingService.makeIntent(context, trackers[i], OTItem.LoggingSource.Shortcut), PendingIntent.FLAG_UPDATE_CURRENT)
                val openItemActivityIntent = PendingIntent.getActivity(context, i, ItemEditingActivity.makeIntent(trackers[i].objectId, context), PendingIntent.FLAG_UPDATE_CURRENT)

                element.setOnClickPendingIntent(R.id.ui_button_instant, instantLoggingIntent)
                element.setOnClickPendingIntent(R.id.group, openItemActivityIntent)
            }

            rv.addView(R.id.container, element)
        }

        return rv
    }

    fun refreshNotificationShortcutViews(user: OTUser, context: Context = OTApplication.app) {

        if (showPanels) {
            val trackers = user.getTrackersOnShortcut()
            if (trackers.isNotEmpty()) {
                val bigView = buildNewNotificationShortcutViews(user, context, true)
                val normalView = buildNewNotificationShortcutViews(user, context, false)

                val noti = NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.icon_simple)
                        .setLargeIcon(VectorIconHelper.getConvertedBitmap(context, R.drawable.icon_simple))
                        .setContentTitle(context.resources.getString(R.string.app_name))
                        .setCustomBigContentView(bigView)
                        .setCustomContentView(normalView)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setAutoCancel(false)
                        .setOngoing(true)
                        .setPriority(Notification.PRIORITY_MAX)
                        .build()

                notificationManager
                        .notify(NOTIFICATION_ID, noti)
            } else {
                //dismiss notification
                notificationManager.cancel(NOTIFICATION_ID)
            }
        } else {
            //dismiss
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }


    fun notifyAppearanceChanged(@Suppress("UNUSED_PARAMETER") tracker: OTTracker) {
        /*
        val intent = Intent(OTApplication.BROADCAST_ACTION_SHORTCUT_TRACKER_INFO_CHANGED)
        intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)

        OTApplication.app.sendBroadcast(intent)*/
        val user = tracker.owner
        if (user != null)
            refreshNotificationShortcutViews(user)
    }

    operator fun plusAssign(@Suppress("UNUSED_PARAMETER") tracker: OTTracker)
    {/*
        val intent = Intent(OTApplication.BROADCAST_ACTION_SHORTCUT_INCLUDE_TRACKER)
        intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)

        OTApplication.app.sendBroadcast(intent)*/

        val user = tracker.owner
        if (user != null) {
            refreshNotificationShortcutViews(user)
        }

        OTApplication.app.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(OTApplication.app))
    }

    operator fun minusAssign(@Suppress("UNUSED_PARAMETER") tracker: OTTracker)
    {
    /*    val intent = Intent(OTApplication.BROADCAST_ACTION_SHORTCUT_EXCLUDE_TRACKER)
        intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)

        OTApplication.app.sendBroadcast(intent)*/
        val user = tracker.owner
        if (user != null)
            refreshNotificationShortcutViews(user)

        OTApplication.app.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(OTApplication.app))
    }

    fun disposeShortcutPanel() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

}