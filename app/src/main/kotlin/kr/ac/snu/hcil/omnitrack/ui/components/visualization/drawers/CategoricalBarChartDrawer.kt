package kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers

import android.graphics.Canvas
import android.graphics.RectF
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.ICategoricalBarChart
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.IChartInterface
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.Axis
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.CategoricalAxisScale
import java.util.*

/**
 * Created by Young-Ho on 9/8/2016.
 */
class CategoricalBarChartDrawer(): AChartDrawer() {

    private var vAxisWidth: Float
    private val hAxisHeight: Float

    private val canvasRect: RectF

    override val aspectRatio: Float = 1.7f

    private val horizontalAxisScale = CategoricalAxisScale()
    private var horizontalAxis = Axis(Axis.Pivot.BOTTOM)

    private val barData = ArrayList<ICategoricalBarChart.Point>()

    init{
        hAxisHeight = OTApplication.app.resources.getDimension(R.dimen.vis_axis_height).toFloat()
        vAxisWidth = OTApplication.app.resources.getDimension(R.dimen.vis_axis_width).toFloat()

        canvasRect = RectF()
        horizontalAxis.scale = horizontalAxisScale
    }

    override fun onResized() {
        canvasRect.set(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat() - hAxisHeight)
        horizontalAxis.attachedTo = canvasRect
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
        }
    }

    override fun onRefresh() {

    }

    override fun onDraw(canvas: Canvas) {
        println("draw categorical chart")
        horizontalAxis.onDraw(canvas)
    }

}