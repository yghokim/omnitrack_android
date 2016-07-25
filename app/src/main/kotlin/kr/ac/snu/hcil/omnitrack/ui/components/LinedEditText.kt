package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.widget.EditText
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by younghokim on 16. 7. 24..
 */
class LinedEditText : EditText {

    constructor(context: Context?) : super(context) {
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    }

    private var numLines: Int = 1
    private val lineBounds = Rect()
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var mLineSpacingExtra: Float = 0.0f
    private var mLineSpacingMultiplier: Float = 0.0f


    init {
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 2.0f
        linePaint.color = resources.getColor(R.color.editTextLine, null)

        mLineSpacingExtra = lineSpacingExtra
        mLineSpacingMultiplier = lineSpacingMultiplier

        addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                this@LinedEditText.setLineSpacing(0f, 1f)
                this@LinedEditText.setLineSpacing(mLineSpacingExtra, mLineSpacingMultiplier)
            }

        })
    }

    override fun onDraw(canvas: Canvas) {

        var baseline = getLineBounds(0, lineBounds) + 5 * resources.displayMetrics.density - lineHeight; // start from upline

        for (i in 0..numLines) {

            canvas.drawLine(lineBounds.left.toFloat(), baseline.toFloat(), lineBounds.right.toFloat(), baseline.toFloat(), linePaint);
            baseline += lineHeight
        }

        super.onDraw(canvas);
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            numLines = height / lineHeight
        }
    }
}