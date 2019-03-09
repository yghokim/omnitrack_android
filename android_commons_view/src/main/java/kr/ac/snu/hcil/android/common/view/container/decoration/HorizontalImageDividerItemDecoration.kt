package kr.ac.snu.hcil.omnitrack.ui.components.decorations

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by Young-Ho Kim on 2016-07-25.
 */
class HorizontalImageDividerItemDecoration(@DrawableRes resId: Int, context: Context, heightMultiplier: Float = 1.0f) : RecyclerView.ItemDecoration() {

    private var divider: Drawable = ContextCompat.getDrawable(context, resId)!!

    private var dividerHeight: Int = 0

    init {
        dividerHeight = (divider.intrinsicHeight * heightMultiplier).toInt()
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {

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

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        if (parent.getChildAdapterPosition(view) != 0)
            outRect.top = dividerHeight
    }
}
