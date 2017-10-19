package kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers

import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.visualization.Granularity
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.Axis
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.QuantizedTimeScale

/**
 * Created by Young-Ho on 9/9/2016.
 */
abstract class ATimelineChartDrawer : AChartDrawer() {

    val xScale = QuantizedTimeScale().inset(true)

    val horizontalAxis = Axis(Axis.Pivot.BOTTOM)

    init {
        paddingBottom = OTApp.instance.resourcesWrapped.getDimension(R.dimen.vis_axis_height).toFloat()
        paddingLeft = OTApp.instance.resourcesWrapped.getDimension(R.dimen.vis_axis_width).toFloat()
        paddingTop = OTApp.instance.resourcesWrapped.getDimension(R.dimen.vis_axis_label_numeric_size).toFloat()
        paddingRight = OTApp.instance.resourcesWrapped.getDimension(R.dimen.vis_right_margin).toFloat()

        horizontalAxis.drawBar = true
        horizontalAxis.drawGridLines = true
        horizontalAxis.labelPaint.isFakeBoldText = true


        horizontalAxis.scale = xScale


        children.add(horizontalAxis)
    }

    override fun onRefresh() {

        if (model != null) {
            val timeScope = model!!.getTimeScope()
            val granularity = model!!.getCurrentScopeGranularity()

            xScale.setDomain(timeScope.from, timeScope.to).quantize(granularity)

            if (granularity != Granularity.WEEK && granularity != Granularity.WEEK_REL) {
                horizontalAxis.style = Axis.TickLabelStyle.Small
            } else {
                horizontalAxis.style = Axis.TickLabelStyle.Normal
            }
        }
    }

    override fun onResized() {
        horizontalAxis.attachedTo = plotAreaRect


    }
}