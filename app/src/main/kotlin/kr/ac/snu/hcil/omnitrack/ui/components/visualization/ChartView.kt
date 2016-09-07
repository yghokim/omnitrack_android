package kr.ac.snu.hcil.omnitrack.ui.components.visualization

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 9/7/2016.
 */
class ChartView: View {


    var chartDrawer: AChartDrawer? by Delegates.observable(null as AChartDrawer?){
        prop, old, new->
            if(old!=new)
            {
                var needResize = true
                if(old!=null && new != null)
                {
                    if(old.aspectRatio == new.aspectRatio)
                    {
                        needResize = false
                    }
                }
                chartDrawer?.setView(this)
                if(needResize)
                    requestLayout()
            }
    }

    private var contentWidth: Int = 0
    private var contentHeight: Int = 0


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        var measuredWidth: Int = 0
        var measuredHeight: Int = 0

        measuredWidth = widthSize;
        measuredHeight = (widthSize / (chartDrawer?.aspectRatio ?: 1f) + 0.5f).toInt()

        setMeasuredDimension(measuredWidth, measuredHeight)
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        chartDrawer?.setCanvasSize(w - paddingLeft - paddingRight, h - paddingTop - paddingBottom)
    }


    override fun onDraw(canvas: Canvas) {
        println("chartview onDraw")
        canvas.translate(paddingLeft.toFloat(), paddingRight.toFloat())
        chartDrawer?.onDraw(canvas)
    }

}