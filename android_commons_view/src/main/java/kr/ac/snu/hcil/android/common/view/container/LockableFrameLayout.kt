package kr.ac.snu.hcil.android.common.view.container

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import kotlin.properties.Delegates

/**
 * Created by younghokim on 16. 7. 22..
 */
open class LockableFrameLayout : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var locked: Boolean by Delegates.observable(false) {
        prop, old, new ->
        if (old != new) {
            if (new) {
                onViewLocked()
            } else {
                onViewUnlocked()
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return if (locked) {
            true
        } else {
            super.onInterceptTouchEvent(ev)
        }
    }

    protected open fun onViewLocked() {

    }

    protected open fun onViewUnlocked() {

    }
}