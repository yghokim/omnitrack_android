package kr.ac.snu.hcil.omnitrack.ui.components.visualization

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.ITimelineChart
import kr.ac.snu.hcil.omnitrack.ui.components.common.choice.SelectionView
import kr.ac.snu.hcil.omnitrack.utils.events.IEventListener
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 9/7/2016.
 */
class ChartView : LinearLayout, IEventListener<ChartModel<*>?>, View.OnClickListener {

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
    var model: ChartModel<*>? by Delegates.observable(null as ChartModel<*>?)
    {
        prop, old, new ->
        if (new != null) {
            if (new is ITimelineChart) {
                timeNavigator.visibility = View.VISIBLE
                if (new.isScopeControlSupported) {
                    scopeSelectionView.visibility = View.VISIBLE
                } else scopeSelectionView.visibility = View.GONE
            } else {
                timeNavigator.visibility = View.GONE
                scopeSelectionView.visibility = View.GONE
            }

            chartView.chartDrawer = new.getChartDrawer()
            chartView.chartDrawer?.model = new
            chartView.invalidate()
        }
    }

    private val chartView: ChartCanvasView
    private val timeNavigator: View
    private val leftNavigationButton: View
    private val rightNavigationButton: View

    private val scopeSelectionView: SelectionView


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        orientation = VERTICAL
        inflateContent(R.layout.component_chart_view, true)

        chartView = findViewById(R.id.ui_chart) as ChartCanvasView
        timeNavigator = findViewById(R.id.ui_time_navigation)
        leftNavigationButton = findViewById(R.id.ui_navigate_left)
        rightNavigationButton = findViewById(R.id.ui_navigate_right)

        scopeSelectionView = findViewById(R.id.ui_scope_selection) as SelectionView
        scopeSelectionView.setValues(ITimelineChart.Granularity.values().map { resources.getString(it.nameId) }.toTypedArray())
        scopeSelectionView.onSelectedIndexChanged += {
            sender, index ->

        }
    }

    override fun onEvent(sender: Any, args: ChartModel<*>?) {
        chartView.invalidate()
    }


    override fun onClick(view: View) {
        if (view === leftNavigationButton) {

        } else if (view === rightNavigationButton) {

        }
    }
}