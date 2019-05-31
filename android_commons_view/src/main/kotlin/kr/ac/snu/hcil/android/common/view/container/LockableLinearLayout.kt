package kr.ac.snu.hcil.android.common.view.container

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout
import kr.ac.snu.hcil.android.common.view.InterfaceHelper

class LockableLinearLayout: LinearLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) InterfaceHelper.ALPHA_ORIGINAL else InterfaceHelper.ALPHA_INACTIVE
    }


    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return if (isEnabled) {
            super.onInterceptTouchEvent(ev)
        } else {
            true
        }
    }
}