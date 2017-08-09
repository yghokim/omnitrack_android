package kr.ac.snu.hcil.omnitrack.ui.components.visualization

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*

/**
 * Created by Young-Ho on 9/7/2016.
 */
abstract class AChartDrawer : IDrawer {

    abstract val aspectRatio: Float

    protected var canvasWidth = 0
        private set
    protected var canvasHeight = 0
        private set

    var model: ChartModel<*>? = null
        set(value) {
            if (field != value) {
                field = value
                onModelChanged()
                refresh()

                modelChanged.invoke(this, value)
            }
        }

    val modelChanged = Event<ChartModel<*>?>()

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    protected val children = ArrayList<IDrawer>()

    protected val plotAreaRect: RectF = RectF()

    protected var paddingLeft = 0f
    protected var paddingTop = 0f
    protected var paddingRight = 0f
    protected var paddingBottom = 0f


    init {
        fillPaint.style = Paint.Style.FILL
        strokePaint.style = Paint.Style.STROKE
    }

    fun setCanvasSize(width: Int, height: Int) {
        if (this.canvasWidth != width || this.canvasHeight != height) {
            this.canvasWidth = width
            this.canvasHeight = height
            this.plotAreaRect.set(
                    paddingLeft, paddingTop, width - paddingRight, height - paddingBottom
            )
            onResized()
            refresh()
        }
    }

    protected abstract fun onResized()

    protected abstract fun onModelChanged()


    fun refresh() {
        onRefresh()
    }

    protected abstract fun onRefresh()


    //drawing apis


    protected fun fillRect(rect: RectF, color: Int, canvas: Canvas) {
        fillPaint.color = color
        canvas.drawRect(rect, fillPaint)
    }

    override fun onDraw(canvas: Canvas) {
        for (element in children) {
            element.onDraw(canvas)
        }
    }
}