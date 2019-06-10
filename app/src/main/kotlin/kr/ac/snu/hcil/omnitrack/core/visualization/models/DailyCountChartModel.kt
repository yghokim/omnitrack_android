package kr.ac.snu.hcil.omnitrack.core.visualization.models

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import io.reactivex.Single
import io.realm.Realm
import io.realm.Sort
import kr.ac.snu.hcil.android.common.DataHelper
import kr.ac.snu.hcil.android.common.dipSize
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.types.TimePoint
import kr.ac.snu.hcil.omnitrack.core.visualization.Granularity
import kr.ac.snu.hcil.omnitrack.core.visualization.INativeChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.TrackerChartModel
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.Axis
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.element.BarWithTextElement
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.element.DataEncodedDrawingList
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.NumericScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.QuantizedTimeScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers.ATimelineChartDrawer

/**
 * Created by younghokim on 2017. 5. 8..
 */
class DailyCountChartModel(tracker: OTTrackerDAO, realm: Realm, val context: Context, timeAttribute: OTAttributeDAO? = null) : TrackerChartModel<Pair<Long, Int>>(tracker, realm), INativeChartModel {

    override val name: String
        get() {
            return if (timeAttributeLocalId != null) {
                String.format(context.resources.getString(R.string.msg_vis_daily_count_title_format_with_time_attr), tracker.name, timeAttributeName)
            } else String.format(context.resources.getString(R.string.msg_vis_daily_count_title_format), tracker.name)
        }

    val timeAttributeLocalId: String? = timeAttribute?.localId
    val timeAttributeName: String? = timeAttribute?.name

    private fun getTimestampOfItem(item: OTItemDAO): Long? {
        return if (timeAttributeLocalId == null) {
            item.timestamp
        } else (item.getValueOf(timeAttributeLocalId) as? TimePoint)?.timestamp
    }

    init{
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    override fun reloadData(): Single<List<Pair<Long, Int>>> {
        println("reload chart data. Scope:  ${getTimeScope()}")

        val xScale = QuantizedTimeScale(context)
        xScale.setDomain(getTimeScope().from, getTimeScope().to)
        xScale.quantize(currentGranularity)

        println("reload data for tracker ${tracker.objectId} - DailyCount")
        return if (timeAttributeLocalId != null) {

            Single.just(dbManager.getItemsQueriedWithTimeAttribute(tracker.objectId, getTimeScope(),
                    timeAttributeLocalId, realm).sortedBy { (it.getValueOf(timeAttributeLocalId) as TimePoint).timestamp })
        } else {
            dbManager
                    .makeItemsQuery(tracker.objectId, getTimeScope(), realm)
                    .sort("timestamp", Sort.ASCENDING)
                    .findAllAsync()
                    .asFlowable()
                    .filter { it.isLoaded }
                    .firstOrError()
        }.map { items ->
                    DataHelper.ConvertSortedListToBinWithLong((xScale.binPointsOnDomain + getTimeScope().to).toTypedArray(),
                            items, { item -> getTimestampOfItem(item) ?: 0L }).map { bin -> Pair(bin.x0, bin.values.size) }
                }
    }

    override fun getChartDrawer(): AChartDrawer {
        return DailyCountChartDrawer()
    }

    inner class DailyCountChartDrawer : ATimelineChartDrawer(context) {
        override val aspectRatio: Float = 2f

        private val yAxis = Axis(context, Axis.Pivot.LEFT)
        private val yScale = NumericScale()

        private val textPadding: Float

        private val normalRectColor = ContextCompat.getColor(context, R.color.colorPointed)
        private val todayRectColor = ContextCompat.getColor(context, R.color.colorAccent)
        private val normalTextColor = ContextCompat.getColor(context, R.color.textColorMid)
        private val todayTextColor = ContextCompat.getColor(context, R.color.colorAccent)

        private val todayBackgroundColor = ContextCompat.getColor(context, R.color.editTextFormBackground)

        private val counterBarGroups = DataEncodedDrawingList<Pair<Long, Int>, Void?>()

        private var todayIndex: Int = -1
        private var todayCenterX: Float = 0f

        private val todayPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        init {
            paddingTop = dipSize(context, 17)

            yAxis.style = Axis.TickLabelStyle.Small
            yAxis.drawBar = false
            yAxis.drawGridLines = true
            yAxis.scale = yScale

            horizontalAxis.drawGridLines = false

            textPadding = dipSize(context, 5)

            todayPaint.color = todayBackgroundColor

            children.add(yAxis)
            children.add(counterBarGroups)
        }

        override fun onModelChanged() {

        }

        override fun onRefresh() {
            super.onRefresh()

            todayIndex = xScale.domainIndexContaining(System.currentTimeMillis())
            if (todayIndex != -1) {
                todayCenterX = xScale[xScale.binPointsOnDomain[todayIndex]]
            }

            println("current cached data: ${this@DailyCountChartModel.cachedData}")

            try {
                val minCount = this@DailyCountChartModel.cachedData.minBy { elm -> elm.second }?.second ?: 0
                val maxCount = this@DailyCountChartModel.cachedData.maxBy { elm -> elm.second }?.second ?: 0

                yScale.setDomain(minCount.toFloat(), maxCount.toFloat(), true)
                yScale.nice(true)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

            counterBarGroups.setData(this@DailyCountChartModel.cachedData)

            counterBarGroups.appendEnterSelection { datum ->
                datum.value
                val element = BarWithTextElement<Pair<Long, Int>>()
                element.textPadding = textPadding
                element.rectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                element.rectPaint.style = Paint.Style.FILL

                element.textPaint.style = Paint.Style.FILL
                element.textPaint.color = normalTextColor
                element.textPaint.isFakeBoldText = true

                element.datum = datum.value
                updateDatumToBar(element, datum.value)
                element
            }

            counterBarGroups.updateElement { newItem, element ->
                if (element is BarWithTextElement)
                    updateDatumToBar(element, newItem.value)
            }

            counterBarGroups.removeElements(counterBarGroups.getExitElements())
        }

        private fun updateDatumToBar(bar: BarWithTextElement<Pair<Long, Int>>, datum: Pair<Long, Int>) {
            val centerX = xScale[datum.first]
            val barWidth = xScale.getTickInterval() * 0.9f
            bar.rect.set(centerX - barWidth * .5f, yScale.get(datum.second.toFloat()), centerX + barWidth * .5f, yScale.get(0f))
            bar.text = datum.second.toString()

            val isToday = xScale.domainIndexContaining(System.currentTimeMillis()) == xScale.indexOfBinPoint(datum.first)
            bar.rectPaint.color = if (isToday) {
                todayRectColor
            } else normalRectColor
            bar.textPaint.color = if (isToday) {
                todayTextColor
            } else normalTextColor


            if (this@DailyCountChartModel.getCurrentScopeGranularity() <=
                    Granularity.WEEK_2_REL) {
                bar.textPaint.textSize = dipSize(context, 14)
            } else {
                bar.textPaint.textSize = dipSize(context, 10)
            }
        }

        override fun onResized() {
            super.onResized()
            yAxis.attachedTo = plotAreaRect

            counterBarGroups.onResizedCanvas { datum, column ->

                (column as? DataEncodedDrawingList<Pair<Long, Int>, Void?>)?.onResizedCanvas { count, cell ->
                    if (cell is BarWithTextElement<Pair<Long, Int>>) {
                        cell.datum?.let {
                            updateDatumToBar(cell, it)
                        }
                    }
                }
            }
        }

        override fun onDraw(canvas: Canvas) {
            if (todayIndex != -1) {
                canvas.drawRect(todayCenterX - xScale.getTickInterval() * .5f, plotAreaRect.top, todayCenterX + xScale.getTickInterval() * .5f, plotAreaRect.bottom, todayPaint)
            }

            super.onDraw(canvas)
        }
    }
}