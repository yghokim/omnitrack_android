import android.content.Context
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View
import kr.ac.snu.hcil.omnitrack.utils.dipRound

class ItemSpacingDecoration(private val horizontal: Int, private val vertical: Int) : RecyclerView.ItemDecoration() {

    companion object {
        fun fromDIP(horizontal: Int, vertical: Int, context: Context): ItemSpacingDecoration {
            return ItemSpacingDecoration(dipRound(context, horizontal), dipRound(context, vertical))
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