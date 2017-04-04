package kr.ac.snu.hcil.omnitrack.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.services.OTBackgroundLoggingService
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemEditingActivity

/**
 * Created by younghokim on 2017. 3. 30..
 */
class OTShortcutPanelWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TRACKER_CLICK_EVENT = "kr.ac.snu.hcil.omnitrack.action.APP_WIDGET_SHORTCUT_TRACKER_CLICKED"
        const val EXTRA_CLICK_COMMAND = "clickCommand"
        const val CLICK_COMMAND_ROW = "rowClicked"
        const val CLICK_COMMAND_INSTANT_LOGGING = "instantLoggingClicked"

    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == OTApplication.BROADCAST_ACTION_USER_SIGNED_IN || intent.action == OTApplication.BROADCAST_ACTION_USER_SIGNED_OUT) {
            val updateIntent = Intent(context, OTShortcutPanelWidgetUpdateService::class.java)

            updateIntent.action = when (intent.action) {
                OTApplication.BROADCAST_ACTION_USER_SIGNED_IN -> OTShortcutPanelWidgetUpdateService.ACTION_TO_MAIN_MODE
                OTApplication.BROADCAST_ACTION_USER_SIGNED_OUT -> OTShortcutPanelWidgetUpdateService.ACTION_TO_SIGN_IN_MODE
                else -> OTShortcutPanelWidgetUpdateService.ACTION_TO_SIGN_IN_MODE
            }
            val appWidgetManager = AppWidgetManager.getInstance(context)

            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetManager.getAppWidgetIds(ComponentName(context, OTShortcutPanelWidgetProvider::class.java)))

            context.startService(updateIntent)
        } else if (intent.action == ACTION_TRACKER_CLICK_EVENT) {
            when (intent.getStringExtra(EXTRA_CLICK_COMMAND)) {
                CLICK_COMMAND_ROW -> {
                    val trackerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
                    if (trackerId != null) {
                        context.startActivity(ItemEditingActivity.makeIntent(trackerId, context))
                    }
                }

                CLICK_COMMAND_INSTANT_LOGGING -> {
                    val trackerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
                    if (trackerId != null) {
                        context.startService(OTBackgroundLoggingService.makeIntent(context, trackerId, OTItem.LoggingSource.Shortcut))
                    }
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {

        val intent = Intent(context, OTShortcutPanelWidgetUpdateService::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        intent.action = OTShortcutPanelWidgetUpdateService.ACTION_INITIALIZE

        context.startService(intent)

        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}