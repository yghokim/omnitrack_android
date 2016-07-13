package kr.ac.snu.hcil.omnitrack.ui

import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View

/**
 * Created by Young-Ho Kim on 2016-07-13.
 */
class VerticalSpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        super.getItemOffsets(outRect, view, parent, state)
        if(parent.getChildAdapterPosition(view) != (parent.adapter.itemCount-1))
            outRect.bottom = space
    }
}