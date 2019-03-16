package kr.ac.snu.hcil.android.common.view.container.decoration

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kr.ac.snu.hcil.android.common.dipSize
import kotlin.math.roundToInt

class ItemSpacingDecoration(private val horizontal: Int, private val vertical: Int) : RecyclerView.ItemDecoration() {

    companion object {
        fun fromDIP(horizontal: Int, vertical: Int, context: Context): ItemSpacingDecoration {
            return ItemSpacingDecoration(dipSize(context, horizontal).roundToInt(), dipSize(context, vertical).roundToInt())
        }

        fun fromPixel(horizontal: Int, vertical: Int): ItemSpacingDecoration {
            return ItemSpacingDecoration(horizontal, vertical)
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.left = horizontal / 2
        outRect.right = horizontal / 2
        outRect.top = vertical / 2
        outRect.bottom = vertical / 2
    }
}