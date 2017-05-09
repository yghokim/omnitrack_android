package kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.element

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.Anchor

/**
 * Created by younghokim on 2017. 5. 8..
 */
class BarWithTextElement<T> : ADataEncodedDrawer<T> {

    var text: String = ""
    var rect: RectF = RectF()

    var textPadding: Float = 4f

    var textAnchor: Anchor = Anchor.Top

    var rectPaint: Paint
    var textPaint: Paint

    constructor(rectPaint: Paint?, textPaint: Paint?) {
        this.rectPaint = rectPaint ?: Paint(Paint.ANTI_ALIAS_FLAG)
        this.textPaint = textPaint ?: Paint(Paint.ANTI_ALIAS_FLAG)
    }

    constructor() {
        rectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    }


    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(rect, rectPaint)
        when (textAnchor) {
            Anchor.Top -> {
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(text, rect.centerX(), rect.top - textPadding, textPaint)
            }
            Anchor.Bottom -> {
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(text, rect.centerX(), rect.bottom + textPadding + textPaint.textSize, textPaint)
            }
            Anchor.Right -> {
                textPaint.textAlign = Paint.Align.LEFT
                val textHeight = textPaint.measureText(text)
                canvas.drawText(text, rect.right + textPadding, rect.centerY() - (textHeight * .5f), textPaint)
            }
            Anchor.Left -> {
                textPaint.textAlign = Paint.Align.RIGHT
                val textHeight = textPaint.measureText(text)
                canvas.drawText(text, rect.left - textPadding, rect.centerY() - (textHeight * .5f), textPaint)
            }
        }
    }
}