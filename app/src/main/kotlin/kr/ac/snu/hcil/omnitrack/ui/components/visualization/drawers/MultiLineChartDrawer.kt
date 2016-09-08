package kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers

import android.graphics.Canvas
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.ILineChartOnTime
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.Axis
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.NumericScale
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-08.
 */
class MultiLineChartDrawer() : AChartDrawer() {

    override val aspectRatio: Float = 1.7f

    val horizontalAxis = Axis(Axis.Pivot.BOTTOM)
    val verticalAxis = Axis(Axis.Pivot.LEFT)

    private val verticalAxisScale = NumericScale()
    private val horizontalAxisScale = NumericScale()

    private val data = ArrayList<ILineChartOnTime.LineData>()

    init {

        paddingBottom = OTApplication.app.resources.getDimension(R.dimen.vis_axis_height).toFloat()
        paddingLeft = OTApplication.app.resources.getDimension(R.dimen.vis_axis_width).toFloat()
        paddingTop = OTApplication.app.resources.getDimension(R.dimen.vis_axis_label_numeric_size).toFloat()


        verticalAxis.drawBar = false
        verticalAxis.drawGridLines = true
        horizontalAxis.drawBar = false
        horizontalAxis.drawGridLines = true

        horizontalAxis.scale = horizontalAxisScale
        verticalAxis.scale = verticalAxisScale

        children.add(horizontalAxis)
        children.add(verticalAxis)
    }

    override fun onResized() {
        horizontalAxis.attachedTo = plotAreaRect
        verticalAxis.attachedTo = plotAreaRect
    }

    override fun onModelChanged() {

    }

    override fun onRefresh() {
        if (model is ILineChartOnTime && model != null) {
            println("Model changed")

            data.clear()
            data.addAll(model!!.getDataPoints().map {
                it as ILineChartOnTime.LineData
            })

            println("data : ${data}")

            val dataArray = data.toTypedArray()
            val minValue = ILineChartOnTime.LineData.minValue(*dataArray).toFloat()
            val maxValue = ILineChartOnTime.LineData.maxValue(*dataArray).toFloat()

            println("data ranges from $minValue ~ $maxValue")

            verticalAxisScale.setDomain(minValue, maxValue, true).nice(true)

            val timeScope = (model as ILineChartOnTime).getTimeScope()
            horizontalAxisScale.setDomain(timeScope.from.toFloat(), timeScope.to.toFloat(), false)
        }
    }

    override fun onDraw(canvas: Canvas) {
        fillRect(plotAreaRect, OTApplication.app.resources.getColor(R.color.editTextFormBackground, null), canvas)

        super.onDraw(canvas)
    }

}