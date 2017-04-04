package kr.ac.snu.hcil.omnitrack.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.backend.OTAuthManager
import kr.ac.snu.hcil.omnitrack.ui.pages.SignInActivity

/**
 * Created by younghokim on 2017. 3. 30..
 */
class OTShortcutPanelWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {

        if (OTAuthManager.currentSignedInLevel > OTAuthManager.SignedInLevel.NONE) {

            for (id in appWidgetIds) {
                val rv = RemoteViews(context.packageName, R.layout.remoteview_widget_shortcut_body)
                rv.setViewVisibility(R.id.ui_progress_bar, View.INVISIBLE)

                val intent = Intent(context, OTShortcutPanelWidgetService::class.java)
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)

                rv.setRemoteAdapter(R.id.ui_list, intent)

                appWidgetManager.updateAppWidget(id, rv)
            }
        } else {
            for (id in appWidgetIds) {
                val rv = RemoteViews(context.packageName, R.layout.remoteview_widget_login)

                val signInIntent = PendingIntent.getActivity(context, 0,
                        Intent(context, SignInActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK),
                        PendingIntent.FLAG_CANCEL_CURRENT)

                rv.setOnClickPendingIntent(R.id.ui_button_signin, signInIntent)

                appWidgetManager.updateAppWidget(id, rv)
            }
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}