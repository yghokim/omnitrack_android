package kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.visualization.CompoundAttributeChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.ILineChartOnTime
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.Axis
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.Legend
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.element.DataEncodedDrawingList
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.element.PolyLineElement
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.NumericScale
import kr.ac.snu.hcil.omnitrack.views.color.ColorHelper
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-08.
 */
class MultiLineChartDrawer(context: Context) : ATimelineChartDrawer(context) {

    override val aspectRatio: Float = 1.7f
    val verticalAxis = Axis(context, Axis.Pivot.LEFT)

    private val verticalAxisScale = NumericScale()

    private val data = ArrayList<ILineChartOnTime.TimeSeriesTrendData>()

    private val lineElements = DataEncodedDrawingList<ILineChartOnTime.TimeSeriesTrendData, Void?>()
    private val markerElements = DataEncodedDrawingList<ILineChartOnTime.TimeSeriesTrendData, Void?>()

    private val legend = Legend(context)

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)

        paddingBottom = context.resources.getDimension(R.dimen.vis_axis_height_with_legend)

        linePaint.style = Paint.Style.STROKE

        verticalAxis.drawBar = false
        verticalAxis.drawGridLines = true
        verticalAxis.scale = verticalAxisScale

        children.add(verticalAxis)
        children.add(lineElements)
        children.add(markerElements)
        children.add(legend)
    }

    override fun onResized() {
        super.onResized()
        verticalAxis.attachedTo = plotAreaRect
        legend.attachedTo = plotAreaRect

        lineElements.onResizedCanvas { datum, element ->
            if (element is PolyLineElement)
                refreshPolyLine(datum, element)
        }

        markerElements.onResizedCanvas { datum, element ->
            if (element is PolyLineElement)
                refreshPolyLine(datum, element)
        }

    }

    override fun onModelChanged() {

    }

    override fun onRefresh() {
        super.onRefresh()
        if (model is ILineChartOnTime && model != null) {
            println("Model changed")

            if (model is CompoundAttributeChartModel) {
                legend.entries.clear()
                val colorPalette = ColorHelper.getTrackerColorPalette(context)
                for (attr in (model as CompoundAttributeChartModel).fields.withIndex()) {
                    legend.entries.add(
                            Pair(attr.value.name, colorPalette[attr.index % colorPalette.size])
                    )
                }
                legend.refresh()
            }

            data.clear()
            data.addAll(model!!.getDataPoints().map {
                it as ILineChartOnTime.TimeSeriesTrendData
            })

            println("data : $data")

            val dataArray = data.toTypedArray()
            val minValue = ILineChartOnTime.TimeSeriesTrendData.minValue(*dataArray).toFloat()
            val maxValue = ILineChartOnTime.TimeSeriesTrendData.maxValue(*dataArray).toFloat()

            println("data ranges from $minValue ~ $maxValue")

            verticalAxisScale.setDomain(minValue, maxValue, true).nice(true)

            lineElements.setData(data)
                    .appendEnterSelection {
                        datum ->
                        val lines = PolyLineElement<ILineChartOnTime.TimeSeriesTrendData>(linePaint, markerPaint)

                        lines.thickness = context.resources.getDimension(R.dimen.vis_line_chart_thickness)

                        refreshPolyLine(datum, lines)

                        lines
                    }
                    .updateElement { datum, element ->
                        if (element is PolyLineElement) {
                            refreshPolyLine(datum, element)
                        }
                    }

            lineElements.removeElements(lineElements.getExitElements())

            markerElements.setData(data)
                    .appendEnterSelection {
                        datum ->
                        val lines = PolyLineElement<ILineChartOnTime.TimeSeriesTrendData>(linePaint, markerPaint)
                        lines.drawLine = false
                        lines.drawMarker = true

                        lines.markerRadius = context.resources.getDimension(R.dimen.vis_line_chart_marker_radius)
                        lines.markerThickness = context.resources.getDimension(R.dimen.vis_line_chart_marker_stroke)
                        lines.thickness = context.resources.getDimension(R.dimen.vis_line_chart_thickness)

                        refreshPolyLine(datum, lines)

                        lines
                    }
                    .updateElement { datum, element ->
                        if (element is PolyLineElement) {
                            refreshPolyLine(datum, element)
                        }
                    }

            markerElements.removeElements(markerElements.getExitElements())
        }
    }

    fun refreshPolyLine(datum: IndexedValue<ILineChartOnTime.TimeSeriesTrendData>, element: PolyLineElement<ILineChartOnTime.TimeSeriesTrendData>) {

        val colorPalette = ColorHelper.getTrackerColorPalette(context)
        element.color = colorPalette[datum.index % colorPalette.size]
        element.fitNumPoints(datum.value.points.count())
        for (point in datum.value.points.withIndex()) {
            element.set(point.index, xScale[point.value.first], verticalAxisScale[point.value.second.toFloat()])
        }
        element.refreshPath()
    }

    override fun onDraw(canvas: Canvas) {
        fillRect(plotAreaRect, ContextCompat.getColor(context, R.color.editTextFormBackground), canvas)

        super.onDraw(canvas)
        legend.onDraw(canvas)
    }

}