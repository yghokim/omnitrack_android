package kr.ac.snu.hcil.omnitrack.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-07-25.
 */
class HorizontalDividerItemDecoration(val color: Int, val height: Int) : RecyclerView.ItemDecoration() {

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        paint.style = Paint.Style.STROKE
        paint.color = color
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State?) {

        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight

        val childCount = parent.childCount
        for (i in 0..childCount - 2) {
            val child = parent.getChildAt(i)

            val params = child.layoutParams as RecyclerView.LayoutParams

            val top = child.bottom + params.bottomMargin
            val bottom = top + height

            c.drawLine(left.toFloat(), (top + bottom) * .5f, right.toFloat(), (top + bottom) * .5f, paint)
        }

        super.onDrawOver(c, parent, state)

    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        super.getItemOffsets(outRect, view, parent, state)
        if (parent.getChildAdapterPosition(view) != 0)
            outRect.top = height
    }
}
