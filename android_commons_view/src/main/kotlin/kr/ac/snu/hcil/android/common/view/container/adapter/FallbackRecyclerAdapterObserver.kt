package kr.ac.snu.hcil.android.common.view.container.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView

class FallbackRecyclerAdapterObserver(val emptyView: View, val adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>) : RecyclerView.AdapterDataObserver() {

    init {
        adapter.registerAdapterDataObserver(this)
        refresh()
    }

    fun dispose() {
        adapter.unregisterAdapterDataObserver(this)
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        super.onItemRangeInserted(positionStart, itemCount)
        refresh()
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        super.onItemRangeRemoved(positionStart, itemCount)
        refresh()
    }

    override fun onChanged() {
        super.onChanged()
        refresh()
    }

    private fun refresh() {
        if (adapter.itemCount == 0) {
            emptyView.visibility = View.VISIBLE
        } else {
            emptyView.visibility = View.GONE
        }
    }
}