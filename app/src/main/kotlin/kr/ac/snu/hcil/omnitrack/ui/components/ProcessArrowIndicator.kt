package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
    private val arrowPointPaint: Paint

    private val arrowHeadPath = Path()

    private var arrowPointBottomCenterY: Float = 0f

    private var contentWidth: Int = 0
    private var contentHeight: Int = 0
    private var horizontalCenter: Float = 0f
    private var arrowHeadBottomCenterY: Float = 0f


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        arrowPointRadius = context.resources.displayMetrics.density * 2.5f

        arrowHeadLineLength = arrowPointRadius * 3.5f

        arrowHeadThickness = arrowPointRadius * 1.4f

        intrinsicWidth = (Math.sqrt(2.0) * arrowHeadLineLength + arrowHeadThickness).toFloat()

        arrowHeadPaint = Paint()
        arrowHeadPaint.strokeCap = Paint.Cap.ROUND
        arrowHeadPaint.strokeJoin = Paint.Join.ROUND
        arrowHeadPaint.strokeWidth = arrowHeadThickness
        arrowHeadPaint.style = Paint.Style.STROKE
        arrowHeadPaint.color = arrowPointColor

        arrowPointPaint = Paint()
        arrowPointPaint.style = Paint.Style.FILL
        arrowPointPaint.color = arrowPointColor

        calculateCoords()

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
            measuredWidth = Math.min(intrinsicWidth + paddingStart + paddingEnd, widthSize.toFloat()).toInt()
        } else {
            measuredWidth = intrinsicWidth.toInt() + paddingStart + paddingEnd
        }
        setMeasuredDimension(measuredWidth, heightSize)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (changed) {
            contentWidth = measuredWidth - paddingStart - paddingEnd
            contentHeight = measuredHeight - paddingTop - paddingBottom
            horizontalCenter = paddingStart + contentWidth * .5f

            arrowHeadBottomCenterY = paddingTop + contentHeight - arrowHeadThickness * .5f
            calculateCoords()
        }
    }

    private fun calculateCoords() {
        val headLineLengthX = (1 / Math.sqrt(2.0) * arrowHeadLineLength).toFloat()

        arrowHeadPath.reset()
        arrowHeadPath.moveTo(horizontalCenter - headLineLengthX, arrowHeadBottomCenterY - headLineLengthX)
        arrowHeadPath.lineTo(horizontalCenter, arrowHeadBottomCenterY)
        arrowHeadPath.lineTo(horizontalCenter + headLineLengthX, arrowHeadBottomCenterY - headLineLengthX)


        arrowPointBottomCenterY = arrowHeadBottomCenterY - 2 * headLineLengthX

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawPath(arrowHeadPath, arrowHeadPaint)

        val numPoints = 6
        val pointGapY = (arrowPointBottomCenterY - (paddingTop + arrowPointRadius)) / (numPoints - 1)

        for (pi in 0..numPoints - 1) {
            canvas.drawCircle(horizontalCenter, paddingTop + arrowPointRadius + pi * pointGapY, arrowPointRadius, arrowPointPaint)
        }

    }
}