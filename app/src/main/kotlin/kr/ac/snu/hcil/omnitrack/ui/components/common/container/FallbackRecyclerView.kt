package kr.ac.snu.hcil.omnitrack.ui.components.common.container

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View

/**
 * Created by Young-Ho Kim on 2016-12-06.
 */
class FallbackRecyclerView : RecyclerView {

    var emptyView: View? = null

    private val observer = object : AdapterDataObserver() {

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
            if (adapter != null) {
                if (adapter.itemCount == 0) {
                    emptyView?.visibility = View.VISIBLE
                    this@FallbackRecyclerView.visibility = View.GONE
                } else {

                    emptyView?.visibility = View.GONE
                    this@FallbackRecyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun setAdapter(adapter: Adapter<*>?) {
        this.adapter?.unregisterAdapterDataObserver(observer)
        super.setAdapter(adapter)
        adapter?.registerAdapterDataObserver(observer)
        observer.onChanged()
    }
}