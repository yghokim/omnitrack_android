package kr.ac.snu.hcil.omnitrack.ui.components.visualization

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import io.reactivex.disposables.SerialDisposable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.INativeChartModel
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 9/7/2016.
 */
class NativeChartView : LinearLayout, IChartView {

    private val modelSubscription = SerialDisposable()

    override var model: ChartModel<*>? by Delegates.observable(null as ChartModel<*>?)
    {
        prop, old, new ->
        if (old !== new) {

            if (old != null) {
                modelSubscription.set(null)
                old.recycle()
            }

            if (new != null && new is INativeChartModel) {
                subscribeToModelEvent(new)
                chartView.chartDrawer = new.getChartDrawer()
                chartView.chartDrawer?.model = new
                chartView.invalidate()

            } else {
                throw IllegalArgumentException("model should be INativeChartModel")
            }
        }
    }

    private val chartView: NativeChartCanvasCore


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        orientation = VERTICAL
        inflateContent(R.layout.component_chart_view, true)

        chartView = findViewById(R.id.ui_chart)
    }

    private fun subscribeToModelEvent(model: ChartModel<*>) {
        modelSubscription.set(
                model.stateObservable.subscribe { state ->
                    when (state) {
                        ChartModel.State.Loaded -> {
                            chartView.chartDrawer?.refresh()
                            chartView.invalidate()
                        }
                    }
                }
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        model?.let {
            subscribeToModelEvent(it)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        modelSubscription.set(null)
    }

}