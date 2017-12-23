package kr.ac.snu.hcil.omnitrack.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v4.graphics.ColorUtils
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.configuration.OTConfigurationController
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.utils.VectorIconHelper
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 4. 2..
 */
class OTShortcutPanelWidgetService : RemoteViewsService() {

    companion object {

        val lastLoggedTimeFormat by lazy {
            SimpleDateFormat(OTApp.instance.resourcesWrapped.getString(R.string.msg_tracker_list_time_format))
        }
    }

    @Inject
    lateinit var configController: OTConfigurationController

    override fun onCreate() {
        super.onCreate()
        (application as OTApp).applicationComponent.inject(this)
    }

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val configId = intent.getStringExtra(OTApp.INTENT_EXTRA_CONFIGURATION_ID)
        return PanelWidgetElementFactory(this.applicationContext, configController.getConfiguredContextOf(configId)!!, intent)
    }

    class PanelWidgetElementFactory(val context: Context, val configuredContext: ConfiguredContext, intent: Intent) : RemoteViewsFactory {

        @Inject
        protected lateinit var dbManager: BackendDbManager
        @Inject
        protected lateinit var authManager: OTAuthManager

        private val widgetId: Int

        private var mode: String = OTShortcutPanelWidgetUpdateService.MODE_ALL

        private var trackers = ArrayList<OTTrackerDAO.SimpleTrackerInfo>()

        init {
            widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            configuredContext.configuredAppComponent.inject(this)
        }

        override fun onCreate() {
        }

        override fun getLoadingView(): RemoteViews? {
            return null
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun onDataSetChanged() {
            val userId = authManager.userId
            val pref = OTShortcutPanelWidgetUpdateService.getPreferences(context)
            mode = OTShortcutPanelWidgetUpdateService.getMode(widgetId, configuredContext.configuration.id, pref)
            trackers.clear()
            val realm = dbManager.makeNewRealmInstance()
            if(userId!=null) {
                when (mode) {
                    OTShortcutPanelWidgetUpdateService.MODE_ALL -> trackers.addAll(dbManager.makeTrackersOfUserQuery(userId, realm).findAll().map { it.getSimpleInfo() })
                    OTShortcutPanelWidgetUpdateService.MODE_SHORTCUT -> trackers.addAll(dbManager.makeTrackersOfUserQuery(userId, realm).equalTo("isBookmarked", true).findAll().map { it.getSimpleInfo() })
                    OTShortcutPanelWidgetUpdateService.MODE_SELECTIVE -> {
                        val selectedTrackerIds = OTShortcutPanelWidgetUpdateService.getSelectedTrackerIds(widgetId, configuredContext.configuration.id, pref)?.toTypedArray()
                        if (selectedTrackerIds?.isNotEmpty() == true) {
                            trackers.addAll(dbManager.makeTrackersOfUserQuery(userId, realm).`in`("objectId", selectedTrackerIds).findAll().map { it.getSimpleInfo() })
                        }
                    }
                }
            }
            realm.close()
        }

        override fun hasStableIds(): Boolean {
            return false
        }

        override fun getViewAt(position: Int): RemoteViews? {
            val tracker = trackers[position]
            var lastLoggingTime: Long? = null
            var todayCount: Long? = null

            /*
            val totalItemCount = DatabaseManager.getTotalItemCount(tracker).first().toBlocking().first().first
            if (totalItemCount != 0L) {
                lastLoggingTime = DatabaseManager.getLastLoggingTime(tracker).first().toBlocking().first()
                todayCount = DatabaseManager.getLogCountOfDay(tracker).first().toBlocking().first()
            }*/

            val rv = RemoteViews(context.packageName, R.layout.remoteview_widget_shortcut_list_element)

            rv.setTextViewText(R.id.ui_tracker_name, tracker.name)

            /*
            if (lastLoggingTimeObservable == null) {
                    rv.setTextViewText(R.id.ui_text_statistics, context.getString(R.string.msg_never_logged))
                } else {
                    val text = StringBuilder()

                todayCount?.let {
                        text.append(context.getString(R.string.msg_todays_log)).append(": ").append(it)
                                .append("\n")
                    }

                lastLoggingTimeObservable.let {
                        val dateText = TimeHelper.getDateText(it, context)
                        val timeText = lastLoggedTimeFormat.format(Date(it))

                        text.append(dateText).append(" ").append(timeText)
                    }

                    rv.setTextViewText(R.id.ui_text_statistics, text)
                }*/

            val buttonSize = OTApp.instance.resourcesWrapped.getDimensionPixelSize(R.dimen.app_widget_instant_logging_button_height)
            val buttonRadius = buttonSize * .5f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.style = Paint.Style.FILL

            val buttonBitmap = Bitmap.createBitmap(buttonSize, buttonSize, Bitmap.Config.ARGB_8888)
            val buttonCanvas = Canvas(buttonBitmap)
            paint.color = ColorUtils.setAlphaComponent(tracker.color, 200)
            buttonCanvas.drawCircle(buttonRadius, buttonRadius, buttonRadius, paint)
            rv.setImageViewBitmap(R.id.ui_background_image, buttonBitmap)

            val baseTrackerIntent = Intent().putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
            rv.setOnClickFillInIntent(R.id.ui_clickable_area, Intent(baseTrackerIntent).putExtra(OTShortcutPanelWidgetProvider.EXTRA_CLICK_COMMAND, OTShortcutPanelWidgetProvider.CLICK_COMMAND_ROW))
            rv.setOnClickFillInIntent(R.id.ui_button_instant, Intent(baseTrackerIntent).putExtra(OTShortcutPanelWidgetProvider.EXTRA_CLICK_COMMAND, OTShortcutPanelWidgetProvider.CLICK_COMMAND_INSTANT_LOGGING))

            rv.setImageViewBitmap(R.id.ui_button_instant, VectorIconHelper.getConvertedBitmap(context, R.drawable.instant_add))

            return rv
        }

        override fun getCount(): Int {
            return trackers.size
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun onDestroy() {
            trackers.clear()
        }

    }
}