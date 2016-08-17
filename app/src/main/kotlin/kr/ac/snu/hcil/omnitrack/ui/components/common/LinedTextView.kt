package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.TextView

/**
 * Created by younghokim on 16. 8. 16..
 */
class LinedTextView : TextView {


    val base: LinedTextBase

    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    }

    init {
        base = LinedTextBase(this)
    }

    var lineColor: Int
        get() = base.lineColor
        set(value) {
            base.lineColor = value
        }

    override fun onDraw(canvas: Canvas) {

        base.onDraw(canvas)

        super.onDraw(canvas);
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        base.onLayout(changed, left, top, right, bottom)
    }

    @TargetApi(23)
    override fun setTextAppearance(resId: Int) {
        super.setTextAppearance(resId)
        base.refresh()
    }

    override fun setTextAppearance(context: Context?, resId: Int) {
        super.setTextAppearance(context, resId)
        base.refresh()
    }

    override fun setLineSpacing(add: Float, mult: Float) {
        super.setLineSpacing(add, mult)
        base.refresh()
    }
}