package kr.ac.snu.hcil.omnitrack.ui.components.decorations

import android.graphics.Rect
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View

/**
 * Created by Young-Ho Kim on 2016-07-13.
 */
class BetweenSpacingItemDecoration(private val direction: Int, private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        if (parent.getChildAdapterPosition(view) != 0)
            if (direction == LinearLayoutManager.VERTICAL)
                outRect.top = space
            else
                outRect.left = space
    }
}