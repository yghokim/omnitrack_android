package kr.ac.snu.hcil.omnitrack.ui.components.decorations

import android.graphics.Canvas
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View

/**
 * Created by Young-Ho Kim on 2016-07-25.
 */
abstract class AListBottomSpaceItemDecoration(var height: Int, var reversed: Boolean = false) : RecyclerView.ItemDecoration() {


    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State?) {

        if (parent.childCount > 0) {
            val left = parent.paddingLeft.toFloat()
            val right = parent.width - parent.paddingRight.toFloat()

            val child = parent.getChildAt(if (reversed) {
                0
            } else {
                parent.childCount - 1
            })

            val params = child.layoutParams as RecyclerView.LayoutParams

            val top = child.bottom + child.translationY + params.bottomMargin
            val bottom = top + height

            onDrawBottomSpace(c, left, right, top, bottom)
        }
    }

    protected abstract fun onDrawBottomSpace(c: Canvas, left: Float, right: Float, top: Float, bottom: Float);

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        super.getItemOffsets(outRect, view, parent, state)
        if (parent.getChildAdapterPosition(view) == if (reversed) {
            0
        } else {
            parent.adapter.itemCount - 1
        })
            outRect.bottom = height
    }
}
