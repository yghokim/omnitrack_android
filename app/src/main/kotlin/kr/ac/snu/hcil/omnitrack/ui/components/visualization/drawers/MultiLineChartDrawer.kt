package kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers

import android.graphics.Canvas
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.visualization.Granularity
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.ILineChartOnTime
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.Axis
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.NumericScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.QuantizedTimeScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.TimeLinearScale
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-08.
 */
class MultiLineChartDrawer() : ATimelineChartDrawer() {

    override val aspectRatio: Float = 1.7f
    val verticalAxis = Axis(Axis.Pivot.LEFT)

    private val verticalAxisScale = NumericScale()

    private val data = ArrayList<ILineChartOnTime.LineData>()

    init {

        verticalAxis.drawBar = false
        verticalAxis.drawGridLines = true
        verticalAxis.scale = verticalAxisScale

        children.add(verticalAxis)
    }

    override fun onResized() {
        super.onResized()
        verticalAxis.attachedTo = plotAreaRect
    }

    override fun onModelChanged() {

    }

    override fun onRefresh() {
        super.onRefresh()
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
        }
    }

    override fun onDraw(canvas: Canvas) {
        fillRect(plotAreaRect, OTApplication.app.resources.getColor(R.color.editTextFormBackground, null), canvas)

        super.onDraw(canvas)
    }

}