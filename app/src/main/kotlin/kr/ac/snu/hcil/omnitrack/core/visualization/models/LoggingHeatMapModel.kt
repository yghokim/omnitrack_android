package kr.ac.snu.hcil.omnitrack.core.visualization.models

import android.graphics.Canvas
import android.support.v4.graphics.ColorUtils
import android.text.format.DateUtils
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.visualization.TrackerChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.ITimeBinnedHeatMap
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.Axis
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.IAxisScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.element.DataEncodedDrawingList
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.element.RectElement
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.CategoricalAxisScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.QuantizedTimeScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers.ATimelineChartDrawer
import kr.ac.snu.hcil.omnitrack.utils.getHourOfDay
import kr.ac.snu.hcil.omnitrack.utils.setHourOfDay
import java.util.*

/**
 * Created by Young-Ho on 9/9/2016.
 */


class LoggingHeatMapModel(tracker: OTTracker): TrackerChartModel<ITimeBinnedHeatMap.CounterVector>(tracker), ITimeBinnedHeatMap {

    private var _isLoaded = false
    private val data = ArrayList<ITimeBinnedHeatMap.CounterVector>()

    override fun getDataPoints(): List<ITimeBinnedHeatMap.CounterVector> {
        return data
    }

    override val numDataPoints: Int get() = data.size

    override val isLoaded: Boolean get()= _isLoaded

    val hoursInYBin = 2

    override fun onReload() {

        val xScale = QuantizedTimeScale()
        xScale.setDomain(getTimeScope().from, getTimeScope().to)
        xScale.quantize(currentGranularity)

        /*
        Y scale is 2-hour-length bin, ranged from 0 to 24, bins are 12
         */

        //make 2D array
        val countMatrix = Array<IntArray>(xScale.numTicks){
            index->
                IntArray(24 / hoursInYBin)
        }

        val calendarCache = Calendar.getInstance()

        for(xIndex in 0..xScale.numTicks-1)
        {
           //from this tick to next tick
            val from = xScale.binPointsOnDomain[xIndex]

            val to = if(xIndex < xScale.numTicks-1) xScale.binPointsOnDomain[xIndex + 1]
                else getTimeScope().to

            var currentTime = from
            var binIndex = 0
            while(currentTime< to)
            {
                binIndex = 0
                while(binIndex * hoursInYBin < 24)
                {
                    calendarCache.timeInMillis = currentTime + hoursInYBin * binIndex * DateUtils.HOUR_IN_MILLIS

                    val hourOfDay = calendarCache.getHourOfDay()
                    val queryFrom = calendarCache.timeInMillis
                    val queryTo = queryFrom + hoursInYBin * DateUtils.HOUR_IN_MILLIS
                    countMatrix[xIndex][hourOfDay/hoursInYBin] += OTApplication.app.dbHelper.getLogCountDuring(tracker, queryFrom, queryTo-1)

                    binIndex++
                }

                currentTime += DateUtils.DAY_IN_MILLIS
            }


        }

        var maxValue = Int.MIN_VALUE
        for(x in countMatrix)
        {
            for(y in x)
            {
                maxValue = Math.max(y, maxValue)
            }
        }

        data.clear()
        for(xEntry in countMatrix.withIndex())
        {
            data.add(
            ITimeBinnedHeatMap.CounterVector(xScale.binPointsOnDomain[xEntry.index], xEntry.value.map{ it/maxValue.toFloat()}.toFloatArray())
            )
        }

        _isLoaded = true
    }

    override val name: String
        get() = String.format(OTApplication.app.resources.getString(R.string.msg_vis_logging_heatmap_title_format), tracker.name)

    override fun recycle() {
        data.clear()
    }

    override fun getChartDrawer(): AChartDrawer {
        return HeatMapDrawer()
    }

    override fun getDataPointAt(position: Int): ITimeBinnedHeatMap.CounterVector {
        return data[position]
    }

    inner class HeatMapDrawer: ATimelineChartDrawer(){
        override val aspectRatio: Float = 1.7f

        val verticalAxis = Axis(Axis.Pivot.LEFT)

        val yScale = CategoricalAxisScale()

        var cellColumns = DataEncodedDrawingList<ITimeBinnedHeatMap.CounterVector, Void?>()

        init{

            horizontalAxis.gridLinePaint.color = OTApplication.app.resources.getColor(R.color.frontalBackground, null)
            verticalAxis.gridLinePaint.color = OTApplication.app.resources.getColor(R.color.frontalBackground, null)

            verticalAxis.style = Axis.TickLabelStyle.Small

            horizontalAxis.gridOnBorder = true
            verticalAxis.gridOnBorder = true

            yScale.tickFormat = object: IAxisScale.ITickFormat<Int>{
                override fun format(value: Int, index: Int): String {
                    val hourOfDay = index * hoursInYBin
                    if(hourOfDay%6==0)
                    {
                        return String.format("%02d", hourOfDay) + ":00"
                    }
                    else return ""
                }

            }


            horizontalAxis.drawGridLines = true
            horizontalAxis.drawBar = false

            verticalAxis.drawGridLines = true
            verticalAxis.drawBar = false

            yScale.setCategories(*Array<String>(24/hoursInYBin){
                index->
                    val cal = Calendar.getInstance()
                    cal.setHourOfDay(index * hoursInYBin)
                    (index * hoursInYBin).toString()
            }).inverse()

            verticalAxis.scale = yScale
            children.add(verticalAxis)

            children.add(cellColumns)
        }

        override fun onResized() {
            super.onResized()
            verticalAxis.attachedTo = plotAreaRect

            cellColumns.onResizedCanvas { datum, column ->
                val columnGroup = column as DataEncodedDrawingList<Float, ITimeBinnedHeatMap.CounterVector>
                columnGroup.onResizedCanvas { count, cell ->
                    if (cell is RectElement<Float>)
                    {
                        mapCellRectToSpace(cell, datum.value.time, count.index)
                    }
                }
            }

        }

        override fun onModelChanged() {

        }

        private fun mapCellRectToSpace(cell: RectElement<Float>, x: Long, y: Int) {
            val centerX = xScale[x]
            val centerY = yScale[y]
            val width  = xScale.getTickInterval()- 4
            val height = yScale.getTickInterval()

            cell.bound.set(centerX - width/2, centerY - height/2 - 2, centerX + width/2, centerY + height/2 + 2)
        }

        override fun onRefresh() {
            super.onRefresh()

            if(model is LoggingHeatMapModel)
            {
                cellColumns.setData(this@LoggingHeatMapModel.data)
                cellColumns.appendEnterSelection {
                    datum->
                    println("updating enter selection for datum ${datum}")

                    val cellGroup = DataEncodedDrawingList<Float, ITimeBinnedHeatMap.CounterVector>()
                    cellGroup.setData(
                            datum.value.distribution.toList()
                    )

                    cellGroup.appendEnterSelection {
                        count->
                        val newCell = RectElement<Float>()
                        newCell.color = ColorUtils.setAlphaComponent(OTApplication.app.resources.getColor(R.color.colorPointed, null), (255*count.value + 0.5f).toInt())
                        mapCellRectToSpace(newCell, datum.value.time, count.index)
                        newCell
                    }
                    cellGroup
                }

                cellColumns.updateElement { datum, column ->
                    val rectList = column as DataEncodedDrawingList<Float, ITimeBinnedHeatMap.CounterVector>
                    rectList.setData(datum.value.distribution.toList())
                    rectList.appendEnterSelection {
                        count->
                        val newCell = RectElement<Float>()
                        newCell.color = ColorUtils.setAlphaComponent(OTApplication.app.resources.getColor(R.color.colorPointed, null), (255*count.value + 0.5f).toInt())
                        mapCellRectToSpace(newCell, datum.value.time, count.index)
                        newCell
                    }

                    rectList.updateElement { count, cell ->
                        mapCellRectToSpace(cell as RectElement<Float>, datum.value.time, count.index)
                        cell.color = ColorUtils.setAlphaComponent(OTApplication.app.resources.getColor(R.color.colorPointed, null), (255*count.value + 0.5f).toInt())
                    }

                    rectList.removeElements(rectList.getExitElements())

                }

                cellColumns.removeElements(cellColumns.getExitElements())
            }
        }

        override fun onDraw(canvas: Canvas) {
            fillRect(plotAreaRect, OTApplication.app.resources.getColor(R.color.editTextFormBackground, null), canvas)

            super.onDraw(canvas)
        }

    }
}