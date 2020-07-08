package kr.ac.snu.hcil.android.common.view.text

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * Created by Young-Ho Kim on 16. 8. 16
 */
class LinedTextView : AppCompatTextView {


    val base: LinedTextBase

    constructor(context: Context) : super(context) {
        base = LinedTextBase(this, null, 0)
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        base = LinedTextBase(this, attrs, 0)
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        base = LinedTextBase(this, attrs, defStyleAttr)
        init()
    }

    fun init() {
    }

    var lineColor: Int
        get() = base.lineColor
        set(value) {
            base.lineColor = value
        }

    override fun onDraw(canvas: Canvas) {

        base.onDraw(canvas)

        super.onDraw(canvas)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        base.onLayout(changed)
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

    override fun setLineSpacing(add: Float, mult: Float) {
        super.setLineSpacing(add, mult)
        base.refresh()
    }
}