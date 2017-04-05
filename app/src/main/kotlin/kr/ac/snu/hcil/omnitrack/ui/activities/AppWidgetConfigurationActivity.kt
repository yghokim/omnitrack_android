package kr.ac.snu.hcil.omnitrack.ui.activities

import android.appwidget.AppWidgetManager
import android.os.Bundle

/**
 * Created by Young-Ho Kim on 2017-04-05.
 */
open class AppWidgetConfigurationActivity(val layoutId: Int) : OTActivity() {

    protected var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutId)
        setResult(RESULT_CANCELED)

        intent?.let {
            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        } else {
            onCreateWithWidget(appWidgetId)
        }
    }

    protected open fun onCreateWithWidget(appWidgetId: Int) {
    }
}