package kr.ac.snu.hcil.omnitrack.ui.components.decorations

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.view.View
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-07-25.
 */
class HorizontalImageDividerItemDecoration(resId: Int = R.drawable.horizontal_separator_pattern, context: Context, heightMultiplier: Float = 1.0f) : RecyclerView.ItemDecoration() {

    private lateinit var divider: Drawable

    private var dividerHeight: Int = 0

    init {
        divider = context.resources.getDrawable(resId, null)
        dividerHeight = (divider.intrinsicHeight * heightMultiplier).toInt()
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State?) {

        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight

        val childCount = parent.childCount
        for (i in 0..childCount - 2) {
            val child = parent.getChildAt(i)

            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = Math.round(child.bottom + child.translationY + params.bottomMargin)
            val bottom = top + dividerHeight

            divider.setBounds(left, top, right, bottom)
            divider.draw(c)
        }

        super.onDrawOver(c, parent, state)

    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        super.getItemOffsets(outRect, view, parent, state)
        if (parent.getChildAdapterPosition(view) != 0)
            outRect.top = dividerHeight
    }
}
