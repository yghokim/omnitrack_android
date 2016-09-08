package kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers

import android.graphics.Canvas
import android.graphics.RectF
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.ICategoricalBarChart
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.*
import java.util.*

/**
 * Created by Young-Ho on 9/8/2016.
 */
class CategoricalBarChartDrawer(): AChartDrawer() {

    private var vAxisWidth: Float
    private val hAxisHeight: Float
    private val topPadding: Float

    private val canvasRect: RectF

    override val aspectRatio: Float = 1.7f

    private val horizontalAxisScale = CategoricalAxisScale()
    private var horizontalAxis = Axis(Axis.Pivot.BOTTOM)

    private var verticalAxisScale = NumericScale()

    private var verticalAxis = Axis(Axis.Pivot.LEFT)

    private val barData = ArrayList<ICategoricalBarChart.Point>()

    private val barElements = DataEncodedDrawingList<ICategoricalBarChart.Point>()

    init{
        hAxisHeight = OTApplication.app.resources.getDimension(R.dimen.vis_axis_height).toFloat()
        vAxisWidth = OTApplication.app.resources.getDimension(R.dimen.vis_axis_width).toFloat()
        topPadding = OTApplication.app.resources.getDimension(R.dimen.vis_axis_label_numeric_size).toFloat()


        canvasRect = RectF()
        horizontalAxis.scale = horizontalAxisScale
        verticalAxis.scale = verticalAxisScale
        verticalAxis.drawBar = false
        verticalAxis.drawGridLines = true
        verticalAxis.labelPaint.textSize = OTApplication.app.resources.getDimension(R.dimen.vis_axis_label_numeric_size).toFloat()

        children.add(horizontalAxis)
        children.add(verticalAxis)
        children.add(barElements)

    }

    override fun onResized() {
        canvasRect.set(vAxisWidth, topPadding, canvasWidth.toFloat(), canvasHeight.toFloat() - hAxisHeight)
        horizontalAxis.attachedTo = canvasRect
        verticalAxis.attachedTo = canvasRect

        barElements.onResizedCanvas { datum, bar ->
            if (bar is VerticalBar<ICategoricalBarChart.Point>) {
                val dataX = horizontalAxisScale.getTickCoordAt(datum.index)
                val dataY = verticalAxisScale.convertDomainToRangeScale(datum.value.value.toFloat())
                println("$dataX, $dataY")
                val barWidth = Math.min(
                        horizontalAxisScale.getTickInterval() - OTApplication.app.resources.getDimension(R.dimen.vis_bar_spacing),
                        OTApplication.app.resources.getDimension(R.dimen.vis_bar_max_width)
                )

                bar.bound.set(dataX - barWidth / 2, dataY, dataX + barWidth / 2, canvasRect.bottom)

            }
        }
    }

    override fun onModelChanged() {
        if(model is ICategoricalBarChart && model != null) {
            println("Model changed")
            barData.clear()
            barData.addAll( model!!.getDataPoints().map {
                (it as ICategoricalBarChart.Point)
            })

            horizontalAxisScale.setCategories(*barData.map{
                it.label
            }.toTypedArray())

            val minValue = barData.minWith(ICategoricalBarChart.Point.VALUE_COMPARATOR)?.value?.toFloat() ?: 0f
            val maxValue = barData.maxWith(ICategoricalBarChart.Point.VALUE_COMPARATOR)?.value?.toFloat() ?: 0f

            println("data min: $minValue, max: $maxValue")

            verticalAxisScale.setDomain(minValue, maxValue).nice()

            //refresh data
            barElements.setData(barData).appendEnterSelection {
                datum ->
                println("updating enter selection for datum ${datum}")
                val newBar = VerticalBar<ICategoricalBarChart.Point>()
                newBar.color = OTApplication.app.resources.getColor(R.color.colorPointed, null)
                val dataX = horizontalAxisScale.getTickCoordAt(datum.index)
                val dataY = verticalAxisScale.convertDomainToRangeScale(datum.value.value.toFloat())
                println("$dataX, $dataY")
                newBar.bound.set(dataX - 10, dataY, dataX + 10, canvasRect.bottom)

                newBar
            }
        }
    }

    override fun onRefresh() {

    }

    override fun onDraw(canvas: Canvas) {
        println("draw categorical chart")

        fillRect(canvasRect, OTApplication.app.resources.getColor(R.color.editTextFormBackground, null), canvas)

        super.onDraw(canvas)
    }


}