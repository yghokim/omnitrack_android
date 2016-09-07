package kr.ac.snu.hcil.omnitrack.ui.components.visualization

import android.graphics.Canvas
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.ChartView

/**
 * Created by Young-Ho on 9/7/2016.
 */
abstract class AChartDrawer: IDrawer {

    private var chartView: ChartView? = null

    abstract val aspectRatio: Float

    protected var canvasWidth = 0
    private set
    protected var canvasHeight = 0
    private set

    var model: ChartModel<*>? = null
        set(value)
        {
            if(field != value)
            {
                field = value
                onModelChanged()
                refresh(true)
            }
        }

    fun setView(view: ChartView){
        chartView = view
    }

    fun setCanvasSize(width: Int, height: Int)
    {
        if(this.canvasWidth!= width || this.canvasHeight != height) {
            this.canvasWidth = width
            this.canvasHeight = height
            onResized()
            refresh(false)
        }
    }

    protected abstract fun onResized()

    protected abstract fun onModelChanged()


   fun refresh(redrawView: Boolean = true){
       onRefresh()
       if(redrawView)
        chartView?.invalidate()
   }

    protected abstract fun onRefresh()

}