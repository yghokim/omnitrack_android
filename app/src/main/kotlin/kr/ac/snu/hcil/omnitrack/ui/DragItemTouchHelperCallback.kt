package kr.ac.snu.hcil.omnitrack.ui

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper

/**
 * Created by Young-Ho Kim on 2016-07-21.
 * codes by https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-b9456d2b1aaf#.z8t5ugen2
 */
class DragItemTouchHelperCallback(private val adapter: ItemDragHelperAdapter, private val orderChangeAvailable: Boolean = true, private val swipeAvailable: Boolean = true) : ItemTouchHelper.Callback() {

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