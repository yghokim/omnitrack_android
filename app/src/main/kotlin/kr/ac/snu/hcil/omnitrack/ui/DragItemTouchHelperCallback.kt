package kr.ac.snu.hcil.omnitrack.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-07-21.
 * codes by https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-b9456d2b1aaf#.z8t5ugen2
 */
class DragItemTouchHelperCallback(private val adapter: ItemDragHelperAdapter, context: Context, private val orderChangeAvailable: Boolean = true, private val swipeAvailable: Boolean = true, private val showDragShadow: Boolean = true) : ItemTouchHelper.Callback() {

    private var shadow: Drawable? = null

    init {
        if (showDragShadow)
            shadow = context.resources.getDrawable(R.drawable.shadow, null)
    }

    interface ItemDragHelperAdapter {

        fun onMoveItem(fromPosition: Int, toPosition: Int)

        fun onRemoveItem(position: Int)
    }


    override fun isItemViewSwipeEnabled(): Boolean {
        return swipeAvailable
    }


    override fun isLongPressDragEnabled(): Boolean {
        return orderChangeAvailable
    }

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
            c.save()



            c.clipRect(dX + viewHolder.itemView.left - 40, dY + viewHolder.itemView.top - 40, dX + viewHolder.itemView.right + 40, dY + viewHolder.itemView.bottom + 40)

            c.translate(dX - 40, dY - 40)

            if (showDragShadow) {
                shadow?.bounds = c.clipBounds

                shadow?.draw(c)
            }

            c.restore()
        }
    }

    override fun getMovementFlags(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        adapter.onMoveItem(viewHolder.adapterPosition, target.adapterPosition);
        return true;
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        adapter.onRemoveItem(viewHolder.adapterPosition)
    }


}