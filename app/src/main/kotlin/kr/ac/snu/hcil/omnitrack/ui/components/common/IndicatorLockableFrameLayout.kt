package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import com.github.ybq.android.spinkit.style.ChasingDots

/**
 * Created by younghokim on 16. 8. 22..
 */
class IndicatorLockableFrameLayout : LockableFrameLayout {

    private val indicator: Drawable

    init {
        indicator = ChasingDots()
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onViewLocked() {
        super.onViewLocked()

    }

    override fun onViewUnlocked() {
        super.onViewUnlocked()

    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            val contentWidth = right - left - paddingLeft - paddingRight
            val contentHeight = bottom - top - paddingTop - paddingBottom

            val indicatorAspectRatio = indicator.intrinsicWidth / indicator.intrinsicHeight


        }
    }


}