package kr.ac.snu.hcil.omnitrack.ui.activities

import android.appwidget.AppWidgetManager
import android.os.Bundle

/**
 * Created by Young-Ho Kim on 2017-04-05.
 */
open class AppWidgetConfigurationActivity(val layoutId: Int) : OTActivity() {

    private var _appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    protected val appWidgetId: Int get() = _appWidgetId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutId)
        setResult(RESULT_CANCELED)

        intent?.let {
            _appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        if (_appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        } else {
            onCreateWithWidget(_appWidgetId)
        }
    }

    protected open fun onCreateWithWidget(appWidgetId: Int) {
    }
}