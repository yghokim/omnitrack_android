package kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers

import android.graphics.Canvas
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

    //configuration
    var integerValues: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value == true) {
                    verticalAxisScale.tickFormat = {
                        floatValue ->
                        floatValue.toInt().toString()
                    }
                } else {
                    verticalAxisScale.tickFormat = null
                }
            }
        }

    override val aspectRatio: Float = 1.7f

    private val horizontalAxisScale = CategoricalAxisScale()
    private var horizontalAxis = Axis(Axis.Pivot.BOTTOM)

    private var verticalAxisScale = NumericScale()

    private var verticalAxis = Axis(Axis.Pivot.LEFT)

    private val barData = ArrayList<ICategoricalBarChart.Point>()

    private val barElements = DataEncodedDrawingList<ICategoricalBarChart.Point>()

    init{
        paddingBottom = OTApplication.app.resources.getDimension(R.dimen.vis_axis_height).toFloat()
        paddingLeft = OTApplication.app.resources.getDimension(R.dimen.vis_axis_width).toFloat()
        paddingTop = OTApplication.app.resources.getDimension(R.dimen.vis_axis_label_numeric_size).toFloat()

        verticalAxis.drawBar = false
        verticalAxis.drawGridLines = true

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
        horizontalAxis.attachedTo = plotAreaRect
        verticalAxis.attachedTo = plotAreaRect

        barElements.onResizedCanvas { datum, bar ->
            if (bar is VerticalBar<ICategoricalBarChart.Point>) {
                val dataX = horizontalAxisScale.getTickCoordAt(datum.index)
                val dataY = verticalAxisScale.convertDomainToRangeScale(datum.value.value.toFloat())
                println("$dataX, $dataY")
                val barWidth = Math.min(
                        horizontalAxisScale.getTickInterval() - OTApplication.app.resources.getDimension(R.dimen.vis_bar_spacing),
                        OTApplication.app.resources.getDimension(R.dimen.vis_bar_max_width)
                )

                bar.bound.set(dataX - barWidth / 2, dataY, dataX + barWidth / 2, plotAreaRect.bottom - OTApplication.app.resources.getDimension(R.dimen.vis_bar_axis_spacing))

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

            verticalAxisScale.setDomain(minValue, maxValue, true).nice(integerValues)

            //refresh data
            barElements.setData(barData).appendEnterSelection {
                datum ->
                println("updating enter selection for datum ${datum}")
                val newBar = VerticalBar<ICategoricalBarChart.Point>()
                newBar.color = OTApplication.app.resources.getColor(R.color.colorPointed, null)
                newBar
            }
        }
    }

    override fun onRefresh() {

    }

    override fun onDraw(canvas: Canvas) {

        fillRect(plotAreaRect, OTApplication.app.resources.getColor(R.color.editTextFormBackground, null), canvas)

        super.onDraw(canvas)
    }


}