package kr.ac.snu.hcil.omnitrack.ui.components.decorations

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import kr.ac.snu.hcil.omnitrack.OTApplication

/**
 * Created by younghokim on 16. 8. 14..
 */
class DrawableListBottomSpaceItemDecoration(drawableResourceId: Int, height: Int, reversed: Boolean = false) : AListBottomSpaceItemDecoration(height, reversed) {

    private val drawable: Drawable

    init {
        drawable = ContextCompat.getDrawable(OTApplication.app, drawableResourceId)
        if (super.height == 0) {
            super.height = drawable.intrinsicHeight
        }
    }

    override fun onDrawBottomSpace(c: Canvas, left: Float, right: Float, top: Float, bottom: Float) {
        drawable.setBounds(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        drawable.draw(c)
    }


}