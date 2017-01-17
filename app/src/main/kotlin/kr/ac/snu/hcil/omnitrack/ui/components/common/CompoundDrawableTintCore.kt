package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.applyTint

/**
 * Created by younghokim on 2017. 1. 13..
 */
class CompoundDrawableTintCore() {


    private val LEFT = 0
    private val TOP = 1
    private val RIGHT = 2
    private val BOTTOM = 3

    fun init(context: Context, compoundDrawables: Array<Drawable?>, attrs: AttributeSet, defStyleAttr: Int): Array<Drawable?> {

        /*
        val isRequired = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M
        if (!isRequired) {
            return
        }*/

        val a = context.obtainStyledAttributes(attrs, R.styleable.DrawableTint,
                defStyleAttr, 0)

        try {

            val hasColor = a.hasValue(R.styleable.DrawableTint_drawableTint)
            if (hasColor) {
                val color = a.getColor(R.styleable.DrawableTint_drawableTint,
                        Color.TRANSPARENT)
                compoundDrawables[LEFT] = tint(compoundDrawables[LEFT], color)
                compoundDrawables[TOP] = tint(compoundDrawables[TOP], color)
                compoundDrawables[RIGHT] = tint(compoundDrawables[RIGHT], color)
                compoundDrawables[BOTTOM] = tint(compoundDrawables[BOTTOM], color)
            }
        } finally {
            a.recycle()
        }

        return compoundDrawables
    }

    private fun tint(drawable: Drawable?, color: Int): Drawable? {
        drawable?.let {
            return applyTint(drawable, color)
        }
        return null
    }
}