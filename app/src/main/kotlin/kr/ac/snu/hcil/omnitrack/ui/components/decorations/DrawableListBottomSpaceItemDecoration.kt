package kr.ac.snu.hcil.omnitrack.ui.components.decorations

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import kr.ac.snu.hcil.omnitrack.OTApplication

/**
 * Created by younghokim on 16. 8. 14..
 */
class DrawableListBottomSpaceItemDecoration(drawableResourceId: Int, height: Int) : AListBottomSpaceItemDecoration(height) {

    private val drawable: Drawable

    init {
        drawable = OTApplication.app.resources.getDrawable(drawableResourceId, null)
        if (super.height == 0) {
            super.height = drawable.intrinsicHeight
        }
    }

    override fun onDrawBottomSpace(c: Canvas, left: Float, right: Float, top: Float, bottom: Float) {
        drawable.setBounds(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        drawable.draw(c)
    }


}