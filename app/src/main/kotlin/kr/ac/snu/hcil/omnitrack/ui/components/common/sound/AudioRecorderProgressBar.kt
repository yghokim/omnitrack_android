package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.NumericScale
import java.util.*

/**
 * Created by younghokim on 2016. 9. 28..
 */
class AudioRecorderProgressBar : View {

    private val scale: NumericScale

    private val renderAreaRect: RectF = RectF()
    private val progressedAreaRect: RectF = RectF()

    private val backgroundPaint: Paint

    private val centerLinePaint: Paint

    private val volumeLinePaint: Paint

    private val progressedAreaPaint: Paint

    var currentProgressRatio: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                refreshProgressAreaRectSize()
                invalidate()
            }
        }

    private val volumePoints = ArrayList<Pair<Float, Int>>()

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        scale = NumericScale().setDomain(0f, 1f, true)

        backgroundPaint = Paint()
        backgroundPaint.color = Color.BLACK
        backgroundPaint.alpha = 15

        centerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        centerLinePaint.style = Paint.Style.STROKE
        centerLinePaint.alpha = 50


        progressedAreaPaint = Paint()
        progressedAreaPaint.style = Paint.Style.FILL
        progressedAreaPaint.color = resources.getColor(R.color.colorPointed, null)
        progressedAreaPaint.alpha = 150

        volumeLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        volumeLinePaint.style = Paint.Style.STROKE
        volumeLinePaint.color = Color.WHITE
        volumeLinePaint.alpha = 100
        volumeLinePaint.strokeWidth = resources.displayMetrics.density
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(renderAreaRect, backgroundPaint)


        canvas.drawRect(progressedAreaRect, progressedAreaPaint)

        var x: Float
        var volumeLength: Float
        for (volumePoint in volumePoints) {
            x = scale[volumePoint.first]
            volumeLength = renderAreaRect.height() / 2 * Math.min(0.9f, (volumePoint.second.toFloat() / 8000))
            canvas.drawLine(x, renderAreaRect.centerY() - volumeLength, x, renderAreaRect.centerY() + volumeLength, volumeLinePaint)
        }
        canvas.drawLine(renderAreaRect.left, renderAreaRect.centerY(), renderAreaRect.right, renderAreaRect.centerY(), centerLinePaint)
    }

    private fun refreshProgressAreaRectSize() {
        progressedAreaRect.set(renderAreaRect.left, renderAreaRect.top, scale[currentProgressRatio], renderAreaRect.bottom)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            scale.setRealCoordRange(paddingLeft.toFloat(), (right - left - paddingRight).toFloat())
            renderAreaRect.set(paddingLeft.toFloat(), paddingTop.toFloat(), (right - left - paddingRight).toFloat(), (bottom - top - paddingBottom).toFloat())
            refreshProgressAreaRectSize()
        }
    }

    fun clear() {
        currentProgressRatio = 0f
        volumePoints.clear()
    }

    fun putVolumeBar(ratio: Float = currentProgressRatio, volume: Int) {
        volumePoints.add(Pair(ratio, volume))
    }
}