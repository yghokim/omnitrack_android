package kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers

import android.graphics.Canvas
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.ILineChartOnTime
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.Axis
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.element.DataEncodedDrawingList
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.element.PolyLineElement
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.NumericScale
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-08.
 */
class MultiLineChartDrawer() : ATimelineChartDrawer() {

    override val aspectRatio: Float = 1.7f
    val verticalAxis = Axis(Axis.Pivot.LEFT)

    private val verticalAxisScale = NumericScale()

    private val data = ArrayList<ILineChartOnTime.TimeSeriesTrendData>()

    private val lineElements = DataEncodedDrawingList<ILineChartOnTime.TimeSeriesTrendData, Void?>()

    init {

        verticalAxis.drawBar = false
        verticalAxis.drawGridLines = true
        verticalAxis.scale = verticalAxisScale

        children.add(verticalAxis)
        children.add(lineElements)
    }

    override fun onResized() {
        super.onResized()
        verticalAxis.attachedTo = plotAreaRect

        lineElements.onResizedCanvas { datum, element ->
            if (element is PolyLineElement)
                refreshPolyLine(datum.value, element)
        }
    }

    override fun onModelChanged() {

    }

    override fun onRefresh() {
        super.onRefresh()
        if (model is ILineChartOnTime && model != null) {
            println("Model changed")

            data.clear()
            data.addAll(model!!.getDataPoints().map {
                it as ILineChartOnTime.TimeSeriesTrendData
            })

            println("data : ${data}")

            val dataArray = data.toTypedArray()
            val minValue = ILineChartOnTime.TimeSeriesTrendData.minValue(*dataArray).toFloat()
            val maxValue = ILineChartOnTime.TimeSeriesTrendData.maxValue(*dataArray).toFloat()

            println("data ranges from $minValue ~ $maxValue")

            verticalAxisScale.setDomain(minValue, maxValue, true).nice(true)

            lineElements.setData(data)
                    .appendEnterSelection {
                        datum ->
                        val lines = PolyLineElement<ILineChartOnTime.TimeSeriesTrendData>()

                        refreshPolyLine(datum.value, lines)

                        lines
                    }
                    .updateElement { datum, element ->
                        if (element is PolyLineElement) {
                            refreshPolyLine(datum.value, element)
                        }
                    }

            lineElements.removeElements(lineElements.getExitElements())
        }
    }

    fun refreshPolyLine(datum: ILineChartOnTime.TimeSeriesTrendData, element: PolyLineElement<ILineChartOnTime.TimeSeriesTrendData>) {
        element.fitNumPoints(datum.points.count())
        for (point in datum.points.withIndex()) {
            element.addPoint(xScale[point.value.first], verticalAxisScale[point.value.second.toFloat()])
        }
    }

    override fun onDraw(canvas: Canvas) {
        fillRect(plotAreaRect, OTApplication.app.resources.getColor(R.color.editTextFormBackground, null), canvas)

        super.onDraw(canvas)
    }

}