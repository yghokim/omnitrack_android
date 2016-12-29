package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.components.common.FallbackRecyclerView
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.DrawableListBottomSpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalImageDividerItemDecoration
import rx.subscriptions.CompositeSubscription
import java.util.*

/**
 * Created by Young-Ho Kim on 9/2/2016
 */
abstract class ATriggerListFragmentCore(val parent: Fragment) {

    companion object {
        private const val STATE_EXPANDED_POSITION = "expandedTriggerPosition"
    }

    protected var user: OTUser? = null

    abstract fun makeNewTriggerInstance(type: Int): OTTrigger
    abstract fun getTriggers(): Array<OTTrigger>
    abstract val triggerActionTypeName: Int

    protected abstract val emptyMessageId: Int

    private lateinit var adapter: Adapter

    private lateinit var listView: FallbackRecyclerView

    private lateinit var newTriggerButton: FloatingActionButton

    private var expandedTriggerPosition: Int = -1
    private var expandedViewHolder: ATriggerViewHolder<out OTTrigger>? = null

    private val subscriptions = CompositeSubscription()

    private val triggerTypeDialog: AlertDialog by lazy {
        NewTriggerTypeSelectionDialogHelper.builder(parent.context, triggerActionTypeName) {
            type ->
            println("trigger type selected - $type")

            val newTrigger = makeNewTriggerInstance(type)
            appendNewTrigger(newTrigger)

            triggerTypeDialog.dismiss()
        }.create()
    }

    fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, @Suppress("UNUSED_PARAMETER") savedInstanceState: Bundle?): View {
        val rootView = inflater!!.inflate(R.layout.fragment_tracker_detail_triggers, container, false)

        listView = rootView.findViewById(R.id.ui_trigger_list) as FallbackRecyclerView
        listView.emptyView = (rootView.findViewById(R.id.ui_empty_list_message) as TextView).apply {
            setText(emptyMessageId)
        }

        val layoutManager = LinearLayoutManager(parent.context, LinearLayoutManager.VERTICAL, false)
        //layoutManager.reverseLayout = true

        listView.layoutManager = layoutManager

        //listView.addItemDecoration(HorizontalDividerItemDecoration(resources.getColor(R.color.dividerColor, null), resources.getDimensionPixelSize(R.dimen.trigger_list_element_divider_height)))

        listView.addItemDecoration(HorizontalImageDividerItemDecoration(context = parent.context))
        listView.addItemDecoration(DrawableListBottomSpaceItemDecoration(R.drawable.expanded_view_inner_shadow_top, parent.context.resources.getDimensionPixelSize(R.dimen.tracker_list_bottom_space), false))

        listView.itemAnimator = SlideInLeftAnimator(DecelerateInterpolator(2.0f))

        adapter = Adapter()
        expandedTriggerPosition = savedInstanceState?.getInt(STATE_EXPANDED_POSITION, -1) ?: -1
        listView.adapter = adapter

        newTriggerButton = rootView.findViewById(R.id.ui_button_new_trigger) as FloatingActionButton

        newTriggerButton.setOnClickListener {
            onNewTriggerButtonClicked()
        }

        return rootView
    }

    fun onViewCreated(view: View?, savedInstanceState: Bundle?) {

        subscriptions.add(
                OTApplication.app.currentUserObservable.subscribe {
                    user ->
                    this.user = user
                    adapter.notifyDataSetChanged()
                }
        )
    }

    fun refresh() {
        adapter.notifyDataSetChanged()
    }

    fun onDestroyView() {
        subscriptions.clear()
        adapter.unSubscribeAll()
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_EXPANDED_POSITION, expandedTriggerPosition)
    }

    protected fun appendNewTrigger(trigger: OTTrigger) {
        adapter.notifyItemChanged(adapter.itemCount - 1)
        user?.triggerManager?.putNewTrigger(trigger)

        adapter.notifyItemInserted(adapter.itemCount - 1)
        listView.smoothScrollToPosition(adapter.itemCount - 1)

    }

    protected open fun onNewTriggerButtonClicked() {
        triggerTypeDialog.show()
    }

    inner class Adapter() : RecyclerView.Adapter<ATriggerViewHolder<out OTTrigger>>(), ATriggerViewHolder.ITriggerControlListener {

        private val viewHolders = ArrayList<ATriggerViewHolder<out OTTrigger>>()

        fun unSubscribeAll() {
            for (viewHolder in viewHolders) {
                viewHolder.unSubscribeAll()
            }
            viewHolders.clear()
        }

        override fun getItemViewType(position: Int): Int {
            return getTriggers()[position].typeId
        }

        override fun onBindViewHolder(holder: ATriggerViewHolder<out OTTrigger>, position: Int) {
            holder.bind(getTriggers()[position])
            if (expandedTriggerPosition == position) {
                holder.setIsExpanded(true, false)
                expandedViewHolder = holder
            } else {
                holder.setIsExpanded(false, false)
                /*
                if (expandedTriggerPosition != -1) {
                    holder.itemView.alpha = 0.2f
                    holder.itemView.setBackgroundColor(ContextCompat.getColor(parent.context, R.color.outerBackground))
                }*/
            }

            if (expandedTriggerPosition == position || expandedTriggerPosition == -1) {
                /*
                holder.itemView.alpha = 1.0f
                holder.itemView.setBackgroundColor(ContextCompat.getColor(parent.context, R.color.frontalBackground))*/
            }
        }

        override fun onCreateViewHolder(parentView: ViewGroup, viewType: Int): ATriggerViewHolder<out OTTrigger> {

            return when (viewType) {
                OTTrigger.TYPE_TIME ->
                    TimeTriggerViewHolder(parentView, this, parent.context)
                OTTrigger.TYPE_DATA_THRESHOLD ->
                    EventTriggerViewHolder(parentView, this, parent.context)
                else ->
                    TimeTriggerViewHolder(parentView, this, parent.context)

            }.apply {
                viewHolders.add(this)
            }
        }

        override fun getItemCount(): Int {
            return if (user != null) getTriggers().size else 0
        }


        override fun onTriggerEdited(position: Int) {

        }

        override fun onTriggerRemove(position: Int) {

            if (expandedTriggerPosition == position) {
                val lastExpandedIndex = expandedTriggerPosition
                expandedTriggerPosition = -1
                expandedViewHolder = null
                for (triggerEntry in getTriggers().withIndex()) {
                    if (triggerEntry.index != lastExpandedIndex) {
                        this.notifyItemChanged(triggerEntry.index)
                    }
                }
            }

            user?.triggerManager?.removeTrigger(getTriggers()[position])
            this.notifyItemRemoved(position)
            newTriggerButton.visibility = View.VISIBLE
        }

        override fun onTriggerExpandRequested(position: Int, viewHolder: ATriggerViewHolder<out OTTrigger>) {

            if (expandedViewHolder !== viewHolder)
                expandedViewHolder?.setIsExpanded(false, false)

            newTriggerButton.visibility = View.INVISIBLE

            expandedTriggerPosition = position
            expandedViewHolder = viewHolder

            viewHolder.setIsExpanded(true, true)
        }

        override fun onTriggerCollapse(position: Int, viewHolder: ATriggerViewHolder<out OTTrigger>) {
            expandedTriggerPosition = -1
            newTriggerButton.visibility = View.VISIBLE

            if (expandedViewHolder !== viewHolder)
                expandedViewHolder?.setIsExpanded(false, false)

            viewHolder.setIsExpanded(false, true)
            //adapter.notifyDataSetChanged()
        }
    }

}