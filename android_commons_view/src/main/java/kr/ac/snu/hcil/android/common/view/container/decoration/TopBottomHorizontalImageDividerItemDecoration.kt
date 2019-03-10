package kr.ac.snu.hcil.android.common.view.container.decoration

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kr.ac.snu.hcil.android.common.view.R

/**
 * Created by Young-Ho Kim on 2016-07-25.
 */
class TopBottomHorizontalImageDividerItemDecoration(@DrawableRes upperResId: Int = R.drawable.horizontal_separator_pattern_upper, @DrawableRes underResId: Int = R.drawable.horizontal_separator_pattern_under, context: Context, heightMultiplier: Float = 1.0f) : RecyclerView.ItemDecoration() {

    private var upperDivider: Drawable = ContextCompat.getDrawable(context, upperResId)!!

    var upperDividerHeight: Int = 0
        private set

    private var underDivider: Drawable = ContextCompat.getDrawable(context, underResId)!!

    var underDividerHeight: Int = 0
        private set


    init {
        upperDividerHeight = (upperDivider.intrinsicHeight * heightMultiplier).toInt()
        underDividerHeight = (underDivider.intrinsicHeight * heightMultiplier).toInt()
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {

        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight

        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)

            val params = child.layoutParams as RecyclerView.LayoutParams
            val underTop = Math.round(child.bottom + child.translationY + params.bottomMargin)
            val underBottom = underTop + underDividerHeight

            underDivider.setBounds(left, underTop, right, underBottom)
            underDivider.draw(c)

            val upperBottom = Math.round(child.top + child.translationY - params.topMargin)
            val upperTop = upperBottom - upperDividerHeight

            upperDivider.setBounds(left, upperTop, right, upperBottom)
            upperDivider.draw(c)

        }

        super.onDrawOver(c, parent, state)

    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.top = upperDividerHeight
        outRect.bottom = underDividerHeight
    }
}