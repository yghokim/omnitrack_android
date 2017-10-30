package kr.ac.snu.hcil.omnitrack.core.system

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.support.v4.app.TaskStackBuilder
import android.support.v4.graphics.ColorUtils
import android.view.View
import android.widget.RemoteViews
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.ItemLoggingSource
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.services.OTItemLoggingService
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemDetailActivity
import kr.ac.snu.hcil.omnitrack.utils.VectorIconHelper
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetUpdateService

/**
 * Created by Young-Ho Kim on 9/4/2016
 */
object OTShortcutPanelManager {

    const val NOTIFICATION_ID = 200000

    const val MAX_NUM_SHORTCUTS = 5

    const val ACTION_BOOKMARKED_TRACKERS_CHANGED = "${OTApp.PREFIX_ACTION}.bookmarked_trackers_changed"
    const val INTENT_EXTRA_CURRENT_BOOKMARKED_SNAPSHOT = "bookmarked_list"

    val showPanels: Boolean
        get() {
            return PreferenceManager.getDefaultSharedPreferences(OTApp.instance).getBoolean("pref_show_shortcut_panel", false)
        }

    private val notificationManager: NotificationManager by lazy {
        (OTApp.instance.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
    }

    private fun buildNewNotificationShortcutViews(trackers: List<OTTrackerDAO>, context: Context, bigStyle: Boolean): RemoteViews {
        val rv = RemoteViews(context.packageName, if (bigStyle) R.layout.remoteview_shortcut_notification_big else R.layout.remoteview_shortcut_notification_normal)

        if (bigStyle) {
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

        rv.removeAllViews(R.id.container)

        val buttonSize = OTApp.instance.resourcesWrapped.getDimensionPixelSize(R.dimen.button_height_small)
        val buttonRadius = buttonSize * .5f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL

        for (i in 0..MAX_NUM_SHORTCUTS - 1) {
            val element = RemoteViews(context.packageName, if (bigStyle) R.layout.remoteview_shortcut_notification_element else R.layout.remoteview_shortcut_notification_element_normal)

            if (trackers.size - 1 < i) {
                element.setViewVisibility(R.id.ui_button_instant, View.INVISIBLE)
                element.setViewVisibility(R.id.ui_name, View.INVISIBLE)
            } else {
                element.setViewVisibility(R.id.ui_button_instant, View.VISIBLE)
                element.setViewVisibility(R.id.ui_name, View.VISIBLE)

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

                val instantLoggingIntent = PendingIntent.getService(context, i, OTItemLoggingService.makeLoggingIntent(context, ItemLoggingSource.Shortcut, trackers[i].objectId!!), PendingIntent.FLAG_UPDATE_CURRENT)
                val openItemActivityIntent = PendingIntent.getActivity(context, i, ItemDetailActivity.makeNewItemPageIntent(trackers[i].objectId!!, context), PendingIntent.FLAG_UPDATE_CURRENT)

                element.setOnClickPendingIntent(R.id.ui_button_instant, instantLoggingIntent)
                element.setOnClickPendingIntent(R.id.group, openItemActivityIntent)
            }

            rv.addView(R.id.container, element)
        }

        return rv
    }

    fun refreshNotificationShortcutViews(trackers: List<OTTrackerDAO>, context: Context = OTApp.instance.contextCompat) {

        if (showPanels) {
            if (trackers.isNotEmpty()) {

                val bigView = buildNewNotificationShortcutViews(trackers, context, true)
                val normalView = buildNewNotificationShortcutViews(trackers, context, false)

                val noti = NotificationCompat.Builder(context, OTNotificationManager.CHANNEL_ID_WIDGETS)
                        .setSmallIcon(R.drawable.icon_simple)
                        .setLargeIcon(VectorIconHelper.getConvertedBitmap(context, R.drawable.icon_simple))
                        .setContentTitle(context.resources.getString(R.string.app_name))
                        .setCustomBigContentView(bigView)
                        .setCustomContentView(normalView)
                        .setOnlyAlertOnce(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(false)
                        .setOngoing(true)
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

    }

    operator fun plusAssign(@Suppress("UNUSED_PARAMETER") tracker: OTTracker) {/*
        val intent = Intent(OTApp.BROADCAST_ACTION_SHORTCUT_INCLUDE_TRACKER)
        intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)

        OTApp.instance.sendBroadcast(intent)*/

        val user = tracker.owner
        if (user != null) {
            //refreshNotificationShortcutViews(user)
        }

        OTApp.instance.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(OTApp.instance))
    }

    operator fun minusAssign(@Suppress("UNUSED_PARAMETER") tracker: OTTracker) {
        /*    val intent = Intent(OTApp.BROADCAST_ACTION_SHORTCUT_EXCLUDE_TRACKER)
            intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)

            OTApp.instance.sendBroadcast(intent)*/
        val user = tracker.owner
        if (user != null)
        //refreshNotificationShortcutViews(user)

            OTApp.instance.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(OTApp.instance))
    }

    fun disposeShortcutPanel() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

}