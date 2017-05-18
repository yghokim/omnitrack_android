package kr.ac.snu.hcil.omnitrack.core.visualization.models

import android.graphics.Canvas
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.database.DatabaseManager
import kr.ac.snu.hcil.omnitrack.core.visualization.Granularity
import kr.ac.snu.hcil.omnitrack.core.visualization.TrackerChartModel
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.Axis
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.element.BarWithTextElement
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.element.DataEncodedDrawingList
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.NumericScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.QuantizedTimeScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers.ATimelineChartDrawer
import kr.ac.snu.hcil.omnitrack.utils.DataHelper
import rx.internal.util.SubscriptionList
import java.util.*

/**
 * Created by younghokim on 2017. 5. 8..
 */
class DailyCountChartModel(tracker: OTTracker) : TrackerChartModel<Pair<Long, Int>>(tracker) {

    private val dataPoints = ArrayList<Pair<Long, Int>>()
    private var _isLoaded = false
    private var subscriptions = SubscriptionList()

    override fun getDataPointAt(position: Int): Pair<Long, Int> {
        return dataPoints[position]
    }

    override fun getDataPoints(): List<Pair<Long, Int>> {
        return dataPoints
    }

    override val numDataPoints: Int get() = dataPoints.size

    override val name: String = String.format(OTApplication.app.getString(R.string.msg_vis_daily_count_title_format), tracker.name)

    override val isLoaded: Boolean
        get() = _isLoaded

    override fun onReload(finished: (Boolean) -> Unit) {
        subscriptions.clear()

        val xScale = QuantizedTimeScale()
        xScale.setDomain(getTimeScope().from, getTimeScope().to)
        xScale.quantize(currentGranularity)

        subscriptions.add(
                DatabaseManager.loadItems(tracker, getTimeScope(), DatabaseManager.Order.ASC).subscribe({
                    items ->
                    val binnedData = DataHelper.ConvertSortedListToBinWithLong((xScale.binPointsOnDomain + getTimeScope().to).toTypedArray(),
                            items, { item -> item.timestamp })

                    synchronized(dataPoints)
                    {
                        dataPoints.clear()
                        dataPoints.addAll(binnedData.map { bin -> Pair(bin.x0, bin.values.size) })
                    }

                    _isLoaded = true
                    finished.invoke(true)
                }, { finished.invoke(false) })
        )
    }

    override fun recycle() {
        dataPoints.clear()
        _isLoaded = false
        subscriptions.clear()
    }

    override fun getChartDrawer(): AChartDrawer {
        return DailyCountChartDrawer()
    }

    inner class DailyCountChartDrawer() : ATimelineChartDrawer() {
        override val aspectRatio: Float = 2f

        private val yAxis = Axis(Axis.Pivot.LEFT)
        private val yScale = NumericScale()

        private val textPadding: Float

        private val normalRectColor = ContextCompat.getColor(OTApplication.app, R.color.colorPointed)
        private val todayRectColor = ContextCompat.getColor(OTApplication.app, R.color.colorAccent)
        private val normalTextColor = ContextCompat.getColor(OTApplication.app, R.color.textColorMid)
        private val todayTextColor = ContextCompat.getColor(OTApplication.app, R.color.colorAccent)

        private val todayBackgroundColor = ContextCompat.getColor(OTApplication.app, R.color.editTextFormBackground)


        private val counterBarGroups = DataEncodedDrawingList<Pair<Long, Int>, Void?>()

        private var todayIndex: Int = -1
        private var todayCenterX: Float = 0f

        private val todayPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        init {
            paddingTop = 17 * OTApplication.app.resources.displayMetrics.density

            yAxis.style = Axis.TickLabelStyle.Small
            yAxis.drawBar = false
            yAxis.drawGridLines = true
            yAxis.scale = yScale

            horizontalAxis.drawGridLines = false

            textPadding = 5 * OTApplication.app.resources.displayMetrics.density

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

            val minCount = this@DailyCountChartModel.dataPoints.minBy { elm -> elm.second }?.second ?: 0
            val maxCount = this@DailyCountChartModel.dataPoints.maxBy { elm -> elm.second }?.second ?: 0

            yScale.setDomain(minCount.toFloat(), maxCount.toFloat(), true)
            yScale.nice(true)

            counterBarGroups.setData(this@DailyCountChartModel.dataPoints)

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
                bar.textPaint.textSize = 14 * OTApplication.app.resources.displayMetrics.density
            } else {
                bar.textPaint.textSize = 10 * OTApplication.app.resources.displayMetrics.density
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