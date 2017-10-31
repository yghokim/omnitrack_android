package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_multibutton_single_recyclerview.*
import kotlinx.android.synthetic.main.sortable_list_element.view.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.DragItemTouchHelperCallback
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.utils.IReadonlyObjectId
import org.jetbrains.anko.contentView

/**
 * Created by younghokim on 2017. 10. 31..
 */
class TrackerReorderActivity: MultiButtonActionBarActivity(R.layout.activity_multibutton_single_recyclerview) {

    private lateinit var viewModel: OrderedTrackerListViewModel
    private val adapter: Adapter = Adapter()
    private lateinit var touchHelper: ItemTouchHelper

    private val currentTrackerViewModelList = ArrayList<OrderedTrackerListViewModel.OrderedTrackerViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contentView?.setBackgroundColor(ContextCompat.getColor(this, R.color.outerBackground))

        ui_list.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        touchHelper = ItemTouchHelper(DragItemTouchHelperCallback(adapter, this, true, false, true))
        touchHelper.attachToRecyclerView(ui_list)
        ui_list.adapter = adapter

        viewModel = ViewModelProviders.of(this).get(OrderedTrackerListViewModel::class.java)

        creationSubscriptions.add(
                signedInUserObservable.subscribe {
                    userId->
                    viewModel.userId = userId
                }
        )

        creationSubscriptions.add(
                viewModel.orderedTrackerViewModels.subscribe { list ->
                    val diffResult = DiffUtil.calculateDiff(IReadonlyObjectId.DiffUtilCallback(currentTrackerViewModelList, list))
                    currentTrackerViewModelList.clear()
                    currentTrackerViewModelList.addAll(list)
                    diffResult.dispatchUpdatesTo(adapter)
                }
        )
    }

    override fun onToolbarLeftButtonClicked() {
        finish()
    }

    override fun onToolbarRightButtonClicked() {
        viewModel.applyOrders()
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.anim_noop, R.anim.anim_slide_down)
    }

    inner class Adapter : RecyclerView.Adapter<TrackerViewHolder>(), DragItemTouchHelperCallback.ItemDragHelperAdapter {
        override fun onMoveItem(fromPosition: Int, toPosition: Int) {
            viewModel.moveTracker(fromPosition, toPosition)
        }

        override fun onRemoveItem(position: Int) {

        }

        override fun onBindViewHolder(holder: TrackerViewHolder?, position: Int) {
            holder?.title = currentTrackerViewModelList[position].name
            holder?.setColor(currentTrackerViewModelList[position].color)
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): TrackerViewHolder {
            val view = LayoutInflater.from(parent?.context).inflate(R.layout.sortable_list_element, parent, false)
            return TrackerViewHolder(view)
        }

        override fun getItemCount(): Int {
            return currentTrackerViewModelList.size
        }

    }

    inner class TrackerViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnTouchListener {
        var title: String
            get() {
                return itemView.ui_text.text.toString()
            }
            set(value) {
                itemView.ui_text.text = value
            }

        fun setColor(color: Int) {
            itemView.color_bar.setBackgroundColor(color)
        }

        init {
            itemView.setOnTouchListener(this)
        }

        override fun onTouch(view: View, mv: MotionEvent): Boolean {
            if (mv.x <= itemView.ui_drag_handle.right || mv.x >= itemView.ui_drag_handle_right.left) {
                if (mv.action == MotionEvent.ACTION_DOWN) {
                    touchHelper.startDrag(this)
                    return true
                }
            }
            return false
        }
    }
}