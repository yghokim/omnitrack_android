package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Created by Young-Ho on 8/7/2016.
 */
class ProcessArrowIndicator : View {

    private val arrowPointRadius: Float

    private val arrowHeadLineLength: Float

    private val arrowHeadThickness: Float

    private val arrowPointColor: Int = Color.parseColor("#c2c2c2")

    private val intrinsicWidth: Float

    private val arrowHeadPaint: Paint

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        arrowPointRadius = context.resources.displayMetrics.density * 2.5f

        arrowHeadLineLength = arrowPointRadius * 2.6f

        arrowHeadThickness = arrowPointRadius * 1.4f

        intrinsicWidth = (Math.sqrt(2.0) * arrowHeadLineLength + arrowHeadThickness).toFloat()

        arrowHeadPaint = Paint()
        arrowHeadPaint.strokeCap = Paint.Cap.ROUND
        arrowHeadPaint.strokeJoin = Paint.Join.ROUND
        arrowHeadPaint.style = Paint.Style.STROKE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        var measuredWidth: Int = 0

        if (widthMode == MeasureSpec.EXACTLY) {
            measuredWidth = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            measuredWidth = Math.min(intrinsicWidth, widthSize.toFloat()).toInt()
        } else {
            measuredWidth = intrinsicWidth.toInt()
        }
        setMeasuredDimension(measuredWidth, heightSize)
    }
}