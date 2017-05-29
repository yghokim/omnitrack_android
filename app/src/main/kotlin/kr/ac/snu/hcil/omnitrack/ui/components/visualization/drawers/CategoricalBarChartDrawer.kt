package kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers

import android.graphics.Canvas
import android.support.v4.content.ContextCompat
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.ICategoricalBarChart
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.Axis
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.IAxisScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.element.DataEncodedDrawingList
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.element.RectElement
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.CategoricalAxisScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.NumericScale
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
                    verticalAxisScale.tickFormat = object: IAxisScale.ITickFormat<Float>{
                        override fun format(value: Float, index: Int): String { return value.toInt().toString() }
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

    private val barElements = DataEncodedDrawingList<ICategoricalBarChart.Point, Void?>()

    init{
        paddingBottom = OTApplication.app.resourcesWrapped.getDimension(R.dimen.vis_axis_height).toFloat()
        paddingLeft = OTApplication.app.resourcesWrapped.getDimension(R.dimen.vis_axis_width).toFloat()
        paddingTop = OTApplication.app.resourcesWrapped.getDimension(R.dimen.vis_axis_label_numeric_size).toFloat()

        verticalAxis.drawBar = false
        verticalAxis.drawGridLines = true

        horizontalAxis.scale = horizontalAxisScale
        verticalAxis.scale = verticalAxisScale
        verticalAxis.drawBar = false
        verticalAxis.drawGridLines = true
        verticalAxis.labelPaint.textSize = OTApplication.app.resourcesWrapped.getDimension(R.dimen.vis_axis_label_numeric_size).toFloat()

        children.add(horizontalAxis)
        children.add(verticalAxis)
        children.add(barElements)

    }

    override fun onResized() {
        horizontalAxis.attachedTo = plotAreaRect
        verticalAxis.attachedTo = plotAreaRect

        barElements.onResizedCanvas { datum, bar ->
            if (bar is RectElement<ICategoricalBarChart.Point>) {
                mapBarElementToSpace(datum, bar)
            }
        }
    }

    private fun mapBarElementToSpace(datum: IndexedValue<ICategoricalBarChart.Point>, bar: RectElement<ICategoricalBarChart.Point>)
    {
        val dataX = horizontalAxisScale.getTickCoordAt(datum.index)
        println("bar X : $dataX")
        val dataY = verticalAxisScale[datum.value.value.toFloat()]
        val barWidth = Math.min(
                horizontalAxisScale.getTickInterval() - OTApplication.app.resourcesWrapped.getDimension(R.dimen.vis_bar_spacing),
                OTApplication.app.resourcesWrapped.getDimension(R.dimen.vis_bar_max_width)
        )

        bar.bound.set(dataX - barWidth / 2, dataY, dataX + barWidth / 2, plotAreaRect.bottom - OTApplication.app.resourcesWrapped.getDimension(R.dimen.vis_bar_axis_spacing))

    }

    override fun onModelChanged() {
    }

    override fun onRefresh() {

        if(model is ICategoricalBarChart && model != null) {
            println("Model changed")
            barData.clear()
            barData.addAll( model!!.getDataPoints().map {
                (it as ICategoricalBarChart.Point)
            })

            println("categorical bar data")
            println(barData)

            horizontalAxisScale.setCategories(*barData.map{
                it.label
            }.toTypedArray())

            val minValue = barData.minWith(ICategoricalBarChart.Point.VALUE_COMPARATOR)?.value?.toFloat() ?: 0f
            val maxValue = barData.maxWith(ICategoricalBarChart.Point.VALUE_COMPARATOR)?.value?.toFloat() ?: 0f

            verticalAxisScale.setDomain(minValue, maxValue, true).nice(integerValues)

            //refresh data
            barElements.setData(barData).appendEnterSelection {
                datum ->
                println("updating enter selection for datum ${datum}")
                val newBar = RectElement<ICategoricalBarChart.Point>()
                newBar.color = ContextCompat.getColor(OTApplication.app, R.color.colorPointed)

                mapBarElementToSpace(datum, newBar)

                newBar
            }

            //remove exit
            barElements.removeElements(barElements.getExitElements())

            //update
            barElements.updateElement { datum, drawer ->
                val bar = drawer as RectElement<ICategoricalBarChart.Point>
                mapBarElementToSpace(datum, bar)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {

        fillRect(plotAreaRect, ContextCompat.getColor(OTApplication.app, R.color.editTextFormBackground), canvas)

        super.onDraw(canvas)
    }


}