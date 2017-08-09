package kr.ac.snu.hcil.omnitrack.ui.components.visualization

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.utils.events.IEventListener
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import rx.subscriptions.CompositeSubscription
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

    private val internalSubscriptions = CompositeSubscription()

    var model: ChartModel<*>? by Delegates.observable(null as ChartModel<*>?)
    {
        prop, old, new ->
        if(old !== new)
        {
            if(old!=null)
            {
                internalSubscriptions.clear()
                old.recycle()
            }

            if (new != null) {

                internalSubscriptions.add(
                        new.stateObservable.subscribe { state ->
                            when (state) {
                                ChartModel.State.Loaded -> {
                                    chartView.chartDrawer?.refresh()
                                    chartView.invalidate()
                                }
                            }
                        }
                )
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

        chartView = findViewById(R.id.ui_chart)

    }

    override fun onEvent(sender: Any, args: ChartModel<*>?) {
        chartView.invalidate()
    }

}