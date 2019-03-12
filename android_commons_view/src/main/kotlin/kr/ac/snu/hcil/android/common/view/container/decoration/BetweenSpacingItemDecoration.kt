package kr.ac.snu.hcil.omnitrack.ui.components.decorations

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by Young-Ho Kim on 2016-07-13.
 */
class BetweenSpacingItemDecoration(private val direction: Int, private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        if (parent.getChildAdapterPosition(view) != 0)
            if (direction == RecyclerView.VERTICAL)
                outRect.top = space
            else
                outRect.left = space
    }
}