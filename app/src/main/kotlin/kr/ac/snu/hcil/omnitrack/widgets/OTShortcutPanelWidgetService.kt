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
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseDbHelper
import kr.ac.snu.hcil.omnitrack.utils.TimeHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by younghokim on 2017. 4. 2..
 */
class OTShortcutPanelWidgetService : RemoteViewsService() {

    companion object {
        val lastLoggedTimeFormat by lazy {
            SimpleDateFormat(OTApplication.app.resources.getString(R.string.msg_tracker_list_time_format))
        }
    }

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return PanelWidgetElementFactory(this.applicationContext, intent)
    }

    class PanelWidgetElementFactory(val context: Context, intent: Intent) : RemoteViewsFactory {

        private val widgetId: Int

        private var user: OTUser? = null

        init {
            widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        override fun onCreate() {
            user = null
        }

        private fun loadUser() {
            user = OTApplication.app.currentUserObservable
                    .flatMap { user -> user.crawlAllTrackersAndTriggerAtOnce().toObservable() }.onErrorReturn { ex -> null }
                    .first()
                    .toBlocking().first()

        }

        override fun getLoadingView(): RemoteViews? {
            return null
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun onDataSetChanged() {
            loadUser()
        }

        override fun hasStableIds(): Boolean {
            return false
        }

        override fun getViewAt(position: Int): RemoteViews? {
            val tracker = user?.trackers?.get(position)
            if (tracker != null) {

                val itemSummary = FirebaseDbHelper.getItemListSummary(tracker).first().toBlocking().first()

                val rv = RemoteViews(context.packageName, R.layout.remoteview_widget_shortcut_list_element)

                rv.setTextViewText(R.id.ui_tracker_name, tracker.name)

                if (itemSummary.lastLoggingTime == null) {
                    rv.setTextViewText(R.id.ui_text_statistics, context.getString(R.string.msg_never_logged))
                } else {
                    val text = StringBuilder()

                    itemSummary.todayCount?.let {
                        text.append(context.getString(R.string.msg_todays_log)).append(": ").append(it)
                                .append("   ")
                    }

                    itemSummary.lastLoggingTime?.let {
                        val dateText = TimeHelper.getDateText(it, context).toUpperCase()
                        val timeText = lastLoggedTimeFormat.format(Date(it)).toUpperCase()

                        text.append(dateText).append(" ").append(timeText)
                    }

                    rv.setTextViewText(R.id.ui_text_statistics, text)
                }

                val buttonSize = OTApplication.app.resources.getDimensionPixelSize(R.dimen.button_height_small)
                val buttonRadius = buttonSize * .5f
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                paint.style = Paint.Style.FILL

                val buttonBitmap = Bitmap.createBitmap(buttonSize, buttonSize, Bitmap.Config.ARGB_8888)
                val buttonCanvas = Canvas(buttonBitmap)
                paint.color = ColorUtils.setAlphaComponent(tracker.color, 200)
                buttonCanvas.drawCircle(buttonRadius, buttonRadius, buttonRadius, paint)
                rv.setImageViewBitmap(R.id.ui_background_image, buttonBitmap)

                val baseTrackerIntent = Intent().putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
                rv.setOnClickFillInIntent(R.id.fillButton, Intent(baseTrackerIntent).putExtra(OTShortcutPanelWidgetProvider.EXTRA_CLICK_COMMAND, OTShortcutPanelWidgetProvider.CLICK_COMMAND_ROW))
                rv.setOnClickFillInIntent(R.id.ui_button_instant, Intent(baseTrackerIntent).putExtra(OTShortcutPanelWidgetProvider.EXTRA_CLICK_COMMAND, OTShortcutPanelWidgetProvider.CLICK_COMMAND_INSTANT_LOGGING))

                return rv
            } else return null
        }

        override fun getCount(): Int {
            return user?.trackers?.size ?: 0
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun onDestroy() {
            user = null
        }

    }
}