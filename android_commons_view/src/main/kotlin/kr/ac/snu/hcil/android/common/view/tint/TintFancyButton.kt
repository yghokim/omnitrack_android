package kr.ac.snu.hcil.android.common.view.tint

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import kr.ac.snu.hcil.android.common.view.R
import kr.ac.snu.hcil.android.common.view.applyTint
import mehdi.sakout.fancybuttons.FancyButton

/**
 * Created by younghokim on 2017. 5. 24..
 */
open class TintFancyButton : FancyButton {

    var tintColor: Int? = null
        set(value) {
            if (field != value) {
                field = value

                if (value != null) {
                    applyTint(value)
                }
            }
        }

    constructor(context: Context?) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val c = context.obtainStyledAttributes(attrs, R.styleable.DrawableTint)
        try {
            val tint = c.getColor(R.styleable.DrawableTint_drawableTint, Color.WHITE)
            tintColor = tint

        } finally {
            c.recycle()
        }
    }

    override fun setIconResource(drawable: Int) {
        super.setIconResource(drawable)
        tintColor?.let {
            applyTint(it)
        }
    }

    override fun setIconResource(drawable: Drawable?) {
        super.setIconResource(drawable)
        tintColor?.let {
            applyTint(it)
        }
    }

    private fun applyTint(value: Int) {
        if (this.iconImageObject != null) {
            val drawable = this.iconImageObject.drawable
            if (drawable != null) {
                this.iconImageObject.setImageDrawable(applyTint(drawable, value))
            }
        }
    }

}