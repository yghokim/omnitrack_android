package kr.ac.snu.hcil.omnitrack.widgets

import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.view.View
import android.widget.RemoteViews
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.backend.OTAuthManager
import kr.ac.snu.hcil.omnitrack.ui.pages.SignInActivity

/**
 * Created by Young-Ho Kim on 2017-04-04.
 */
class OTShortcutPanelWidgetUpdateService : Service() {

    companion object {
        const val ACTION_INITIALIZE = "kr.ac.snu.hcil.omnitrack.action.APP_WIDGET_SHORTCUT_INITIALIZE"
        const val ACTION_TO_SIGN_IN_MODE = "kr.ac.snu.hcil.omnitrack.action.APP_WIDGET_SHORTCUT_TO_SIGN_IN"
        const val ACTION_TO_MAIN_MODE = "kr.ac.snu.hcil.omnitrack.action.APP_WIDGET_SHORTCUT_TO_MAIN_MODE"

        fun makeRemoteViewsForNormalMode(context: Context, widgetId: Int? = null): RemoteViews {
            val rv = RemoteViews(context.packageName, R.layout.remoteview_widget_shortcut_body)
            rv.setViewVisibility(R.id.ui_progress_bar, View.INVISIBLE)

            val intent = Intent(context, OTShortcutPanelWidgetService::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId ?: AppWidgetManager.INVALID_APPWIDGET_ID)

            rv.setRemoteAdapter(R.id.ui_list, intent)

            rv.setPendingIntentTemplate(R.id.ui_list, PendingIntent.getBroadcast(context, widgetId ?: AppWidgetManager.INVALID_APPWIDGET_ID,
                    Intent(OTShortcutPanelWidgetProvider.ACTION_TRACKER_CLICK_EVENT), PendingIntent.FLAG_UPDATE_CURRENT))

            return rv
        }

        fun makeRemoteViewsForSignIn(context: Context): RemoteViews {
            val rv = RemoteViews(context.packageName, R.layout.remoteview_widget_login)

            val signInIntent = PendingIntent.getActivity(context, 0,
                    Intent(context, SignInActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK),
                    PendingIntent.FLAG_CANCEL_CURRENT)

            rv.setOnClickPendingIntent(R.id.ui_button_signin, signInIntent)

            return rv
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
            val appWidgetManager = AppWidgetManager.getInstance(this)
            when (intent.action) {
                ACTION_INITIALIZE -> {
                    if (OTAuthManager.currentSignedInLevel > OTAuthManager.SignedInLevel.NONE) {

                        for (id in appWidgetIds) {
                            val rv = makeRemoteViewsForNormalMode(this, id)
                            appWidgetManager.updateAppWidget(id, rv)
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
                    val rv = makeRemoteViewsForNormalMode(this)
                    appWidgetManager.updateAppWidget(appWidgetIds, rv)
                }
            }
        }

        return START_NOT_STICKY
    }

}