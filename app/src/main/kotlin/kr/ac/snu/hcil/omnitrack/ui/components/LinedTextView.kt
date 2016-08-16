package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.TextView

/**
 * Created by younghokim on 16. 8. 16..
 */
class LinedTextView : TextView {

    constructor(context: Context?) : super(context) {
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    }

    private var base: LinedTextBase

    init {
        base = LinedTextBase(this)
    }

    override fun onDraw(canvas: Canvas) {

        base.onDraw(canvas)

        super.onDraw(canvas);
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        base.onLayout(changed, left, top, right, bottom)
    }
}