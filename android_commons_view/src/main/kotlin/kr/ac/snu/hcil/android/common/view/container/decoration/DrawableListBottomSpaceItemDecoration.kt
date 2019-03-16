package kr.ac.snu.hcil.android.common.view.container.decoration

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

/**
 * Created by younghokim on 16. 8. 14..
 */
class DrawableListBottomSpaceItemDecoration(context: Context, @DrawableRes drawableResourceId: Int, height: Int, reversed: Boolean = false) : AListBottomSpaceItemDecoration(height, reversed) {

    private val drawable: Drawable

    init {
        drawable = ContextCompat.getDrawable(context, drawableResourceId)!!
        if (super.height == 0) {
            super.height = drawable.intrinsicHeight
        }
    }

    override fun onDrawBottomSpace(c: Canvas, left: Float, right: Float, top: Float, bottom: Float) {
        drawable.setBounds(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        drawable.draw(c)
    }


}