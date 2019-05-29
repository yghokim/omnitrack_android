package kr.ac.snu.hcil.android.common.view.container

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout

class LockableLinearLayout: LinearLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if(enabled) 1.0f else 0.2f
    }


    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return if (isEnabled) {
            super.onInterceptTouchEvent(ev)
        } else {
            true
        }
    }
}