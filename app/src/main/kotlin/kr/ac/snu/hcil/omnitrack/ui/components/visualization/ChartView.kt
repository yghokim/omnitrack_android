package kr.ac.snu.hcil.omnitrack.ui.components.visualization

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.ui.components.common.choice.SelectionView
import kr.ac.snu.hcil.omnitrack.utils.events.IEventListener
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 9/7/2016.
 */
class ChartView : LinearLayout, IEventListener<ChartModel<*>?> {

    /*
    var chartDrawer: AChartDrawer?
        get() = chartView.chartDrawer
        set(value)
        {
            if(chartDrawer !== value)
            {
                if(chartDrawer!=null)
                {
                    chartDrawer?.modelChanged?.minusAssign(this)
                }
                value?.modelChanged?.plusAssign(this)
            }
            chartView.chartDrawer = value
        }
        */

    val onModelReloaded = {
        sender:Any, success:Boolean->

        chartView.chartDrawer?.refresh()
        chartView.invalidate()
    }

    var model: ChartModel<*>? by Delegates.observable(null as ChartModel<*>?)
    {
        prop, old, new ->
        if(old !== new)
        {
            if(old!=null)
            {
                old.onReloaded -= onModelReloaded
            }

            if (new != null) {
                new.onReloaded += onModelReloaded
                chartView.chartDrawer = new.getChartDrawer()
                chartView.chartDrawer?.model = new
                chartView.invalidate()

            }
        }
    }

    private val chartView: ChartCanvasView


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        orientation = VERTICAL
        inflateContent(R.layout.component_chart_view, true)

        chartView = findViewById(R.id.ui_chart) as ChartCanvasView

    }

    override fun onEvent(sender: Any, args: ChartModel<*>?) {
        chartView.invalidate()
    }

}