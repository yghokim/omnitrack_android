package kr.ac.snu.hcil.omnitrack.ui.components.common.text

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.dipSize
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 16. 8. 16
 */
class LinedTextBase(val textView: View, attrs: AttributeSet?, defStyleAttr: Int = 0) {

    private var numLines: Int = 1
    private val lineBounds = Rect()
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    var lineSpacingExtra: Float = 0.0f
    var lineSpacingMultiplier: Float = 0.0f

    private var lineOffset: Float = 0.0f


    private var lineHeight: Int = 0

    var lineColor: Int
        get() = linePaint.color
        set(value) {
            if (linePaint.color != value) {
                linePaint.color = value
                textView.invalidate()
            }
        }

    var drawOuterLines: Boolean by Delegates.observable(true) {
        prop, old, new ->
        if (old != new) {
            textView.invalidate()
        }
    }


    init {
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 2.0f
        linePaint.color = ContextCompat.getColor(textView.context, R.color.editTextLine)

        lineOffset = dipSize(textView.context, 5)

        val a = textView.context.obtainStyledAttributes(attrs, R.styleable.LinedTextView, defStyleAttr, 0)

        try {
            drawOuterLines = a.getBoolean(R.styleable.LinedTextView_lined_drawOuterLines, true)
        } finally {
            a.recycle()
        }

        refresh()

    }

    fun refresh() {
        lineSpacingExtra = if (textView is TextView) {
            textView.lineSpacingExtra
        } else if (textView is EditText) {
            textView.lineSpacingExtra
        } else 0.0f
        lineSpacingMultiplier = if (textView is TextView) {
            textView.lineSpacingMultiplier
        } else if (textView is EditText) {
            textView.lineSpacingMultiplier
        } else 0.0f

        lineHeight = if (textView is TextView) {
            textView.lineHeight
        } else if (textView is EditText) {
            textView.lineHeight
        } else 0
    }

    private fun getLineBounds(index: Int, bounds: Rect): Int {
        return if (textView is TextView) {
            textView.getLineBounds(index, bounds)
        } else if (textView is EditText) {
            textView.getLineBounds(index, bounds)
        } else 0
    }


    fun onDraw(canvas: Canvas) {
        val baseline = getLineBounds(0, lineBounds) + lineOffset - lineHeight // start from upline

        val start = if (drawOuterLines) {
            0
        } else {
            1
        }
        val end = if (drawOuterLines) {
            numLines
        } else {
            numLines - 1
        }

        for (i in start..end) {
            val y = (baseline + i * lineHeight)
            canvas.drawLine(lineBounds.left.toFloat(), y, lineBounds.right.toFloat(), y, linePaint)
        }
    }

    fun onLayout(changed: Boolean) {
        if (changed) {
            numLines = textView.height / lineHeight
        }
    }


}