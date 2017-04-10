package kr.ac.snu.hcil.omnitrack.widgets

import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.TaskStackBuilder
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.backend.OTAuthManager
import kr.ac.snu.hcil.omnitrack.ui.pages.SignInActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.configs.ShortcutPanelWidgetConfigActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity

/**
 * Created by Young-Ho Kim on 2017-04-04.
 */
class OTShortcutPanelWidgetUpdateService : Service() {

    companion object {
        const val ACTION_INITIALIZE = "kr.ac.snu.hcil.omnitrack.action.APP_WIDGET_SHORTCUT_INITIALIZE"
        const val ACTION_TO_SIGN_IN_MODE = "kr.ac.snu.hcil.omnitrack.action.APP_WIDGET_SHORTCUT_TO_SIGN_IN"
        const val ACTION_TO_MAIN_MODE = "kr.ac.snu.hcil.omnitrack.action.APP_WIDGET_SHORTCUT_TO_MAIN_MODE"
        const val ACTION_NOTIFY_DATA_CHANGED = "kr.ac.snu.hcil.omnitrack.action.APP_WIDGET_SHORTCUT_NOTIFY_DATA_CHANGED"
        const val ACTION_RESIZED = "kr.ac.snu.hcil.omnitrack.action.APP_WIDGET_SHORTCUT_RESIZED"

        const val EXTRA_MODE = "widgetMode"

        const val MODE_SHORTCUT = "mode_shortcuts"
        const val MODE_SELECTIVE = "mode_selective"
        const val MODE_ALL = "mode_all"

        const val EXTRA_SELECTED_TRACKER_IDS = "selectedTrackerIds"

        const val EXTRA_TITLE = "widgetTitle"

        const val EXTRA_OPTIONS = "options"

        const val MINIMUM_HEIGHT_SHOW_HEADER = 110

        fun getPreferences(context: Context): SharedPreferences {
            return context.applicationContext.getSharedPreferences("shortcutPanelWidget", Context.MODE_PRIVATE)
        }

        fun removeVariables(widgetId: Int, editor: SharedPreferences.Editor) {
            editor
                    .remove("${EXTRA_MODE}_${widgetId}")
                    .remove("${EXTRA_TITLE}_${widgetId}")
        }

        fun getMode(widgetId: Int, pref: SharedPreferences): String {
            return pref.getString("${EXTRA_MODE}_${widgetId}", MODE_ALL)
        }

        fun setMode(widgetId: Int, mode: String, editor: SharedPreferences.Editor) {
            editor.putString("${EXTRA_MODE}_${widgetId}", mode)
        }

        fun getTitle(widgetId: Int, pref: SharedPreferences): String {
            return pref.getString("${EXTRA_TITLE}_${widgetId}", "OmniTrack")
        }

        fun setTitle(widgetId: Int, title: String, editor: SharedPreferences.Editor) {
            editor.putString("${EXTRA_TITLE}_${widgetId}", title)
        }

        fun getSelectedTrackerIds(widgetId: Int, pref: SharedPreferences): Set<String>? {
            return pref.getStringSet("${EXTRA_SELECTED_TRACKER_IDS}_${widgetId}", HashSet<String>())
        }

        fun setSelectedTrackerIds(widgetId: Int, value: Set<String>, editor: SharedPreferences.Editor) {
            editor.putStringSet("${EXTRA_SELECTED_TRACKER_IDS}_${widgetId}", value)
        }

        fun makeRemoteViewsForNormalMode(context: Context, widgetId: Int, options: Bundle?): RemoteViews {
            val pref = getPreferences(context)
            val title = getTitle(widgetId, pref)

            val rv = RemoteViews(context.packageName, R.layout.remoteview_widget_shortcut_body)
            rv.setViewVisibility(R.id.ui_progress_bar, View.INVISIBLE)

            options?.let {
                applyOptionsToRemoteViews(rv, it)
            }

            rv.setTextViewText(R.id.ui_app_title, title)

            val intent = Intent(context, OTShortcutPanelWidgetService::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))

            rv.setRemoteAdapter(R.id.ui_list, intent)

            rv.setPendingIntentTemplate(R.id.ui_list, PendingIntent.getBroadcast(context, widgetId,
                    Intent(OTShortcutPanelWidgetProvider.ACTION_TRACKER_CLICK_EVENT), PendingIntent.FLAG_UPDATE_CURRENT))

            /*
            rv.setOnClickPendingIntent(R.id.ui_button_sync,
                    PendingIntent.getService(context, widgetId,
                            OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(context).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId)),
                            PendingIntent.FLAG_UPDATE_CURRENT
                    ))*/

            rv.setOnClickPendingIntent(R.id.ui_button_more,
                    PendingIntent.getActivity(context, widgetId,
                            Intent(context, ShortcutPanelWidgetConfigActivity::class.java)
                                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                            ,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    )
            )

            val stackBuilder = TaskStackBuilder.create(context)
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(HomeActivity::class.java)
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(Intent(context, HomeActivity::class.java))

            val morePendingIntent = stackBuilder.getPendingIntent(widgetId,
                    PendingIntent.FLAG_UPDATE_CURRENT)


            rv.setOnClickPendingIntent(R.id.ui_app_title,
                    morePendingIntent
            )

            return rv
        }

        fun applyOptionsToRemoteViews(rv: RemoteViews, options: Bundle) {
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 9999)
            Log.d("WIDGET", "min height: ${minHeight}")
            if (minHeight < MINIMUM_HEIGHT_SHOW_HEADER) {
                rv.setViewVisibility(R.id.ui_header, View.GONE)
            } else {
                rv.setViewVisibility(R.id.ui_header, View.VISIBLE)
            }
        }

        fun makeRemoteViewsForSignIn(context: Context): RemoteViews {
            val rv = RemoteViews(context.packageName, R.layout.remoteview_widget_login)

            val signInIntent = PendingIntent.getActivity(context, 0,
                    Intent(context, SignInActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK),
                    PendingIntent.FLAG_CANCEL_CURRENT)

            rv.setOnClickPendingIntent(R.id.ui_button_signin, signInIntent)

            return rv
        }

        fun makeNotifyDatesetChangedIntentToAllWidgets(context: Context): Intent {
            val widgetIds = OTShortcutPanelWidgetProvider.getAppWidgetIds(context, null)
            val intent = Intent(context, OTShortcutPanelWidgetUpdateService::class.java)
            intent.action = ACTION_NOTIFY_DATA_CHANGED
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            return intent
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        println("Widget update service action: ${intent.action}")

        val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS) ?: intArrayOf()
        if (appWidgetIds.isNotEmpty()) {
            println("widget ids: ${appWidgetIds.joinToString(", ")}")
            val appWidgetManager = AppWidgetManager.getInstance(this)
            when (intent.action) {
                ACTION_INITIALIZE -> {
                    if (OTAuthManager.currentSignedInLevel > OTAuthManager.SignedInLevel.NONE) {

                        for (id in appWidgetIds) {
                            val options = appWidgetManager.getAppWidgetOptions(id)
                            val rv = makeRemoteViewsForNormalMode(this, id, options)
                            appWidgetManager.updateAppWidget(id, rv)
                            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.ui_list)
                        }
                    } else {
                        for (id in appWidgetIds) {
                            val rv = makeRemoteViewsForSignIn(this)
                            appWidgetManager.updateAppWidget(id, rv)
                        }
                    }
                }

                ACTION_TO_SIGN_IN_MODE -> {
                    val rv = makeRemoteViewsForSignIn(this)
                    appWidgetManager.updateAppWidget(appWidgetIds, rv)
                }

                ACTION_TO_MAIN_MODE -> {
                    for (id in appWidgetIds) {
                        val rv = makeRemoteViewsForNormalMode(this, id, appWidgetManager.getAppWidgetOptions(id))
                        appWidgetManager.updateAppWidget(id, rv)
                    }
                }

                ACTION_NOTIFY_DATA_CHANGED -> {
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.ui_list)
                }

                ACTION_RESIZED -> {
                    if (OTAuthManager.currentSignedInLevel > OTAuthManager.SignedInLevel.NONE) {
                        for (id in appWidgetIds) {
                            val options = appWidgetManager.getAppWidgetOptions(id)
                            val rv = RemoteViews(this.packageName, R.layout.remoteview_widget_shortcut_body)
                            applyOptionsToRemoteViews(rv, options)

                            appWidgetManager.partiallyUpdateAppWidget(id, rv)
                        }
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

}