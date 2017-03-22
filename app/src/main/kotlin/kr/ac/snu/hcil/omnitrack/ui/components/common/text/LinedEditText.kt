package kr.ac.snu.hcil.omnitrack.ui.components.common.text

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.widget.EditText

/**
 * Created by Young-Ho Kim on 16. 7. 24
 */
class LinedEditText : EditText {

    val base: LinedTextBase

    constructor(context: Context?) : super(context) {

        base = LinedTextBase(this, null, 0)
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        base = LinedTextBase(this, attrs, 0)
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        base = LinedTextBase(this, attrs, defStyleAttr)
        init()
    }

    fun init() {
        addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val lineSpacingExtra = base.lineSpacingExtra
                val lineSpacingMultiplier = base.lineSpacingMultiplier

                this@LinedEditText.setLineSpacing(0f, 1f)
                this@LinedEditText.setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
            }

        })
    }

    override fun onDraw(canvas: Canvas) {

        base.onDraw(canvas)

        super.onDraw(canvas)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        base.onLayout(changed)
    }

    override fun setLineSpacing(add: Float, mult: Float) {
        super.setLineSpacing(add, mult)
        base.refresh()
    }

    @TargetApi(23)
    override fun setTextAppearance(resId: Int) {
        super.setTextAppearance(resId)
        base.refresh()
    }

    @Suppress("DEPRECATION")
    override fun setTextAppearance(context: Context?, resId: Int) {
        super.setTextAppearance(context, resId)
        base.refresh()
    }
}