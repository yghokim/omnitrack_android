package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
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
import kr.ac.snu.hcil.omnitrack.core.database.EventLoggingManager
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseHelper
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.FallbackRecyclerView
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.DrawableListBottomSpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalImageDividerItemDecoration
import net.i2p.android.ext.floatingactionbutton.FloatingActionButton
import rx.subscriptions.CompositeSubscription
import java.util.*

/**
 * Created by Young-Ho Kim on 9/2/2016
 */
abstract class ATriggerListFragmentCore(val parent: Fragment) {

    companion object {
        // private const val STATE_EXPANDED_POSITION = "expandedTriggerPosition"
        const val DETAIL_REQUEST_CODE = 12214
    }

    interface TriggerAdapter {
        fun getTriggerAt(position: Int): OTTrigger
        fun triggerCount(): Int

        fun makeNewTriggerInstance(type: Int): OTTrigger

        //user?.triggerManager?.removeTrigger(trigger)
        fun onRemoveTrigger(trigger: OTTrigger)

        //user?.triggerManager?.putNewTrigger(trigger)
        fun onAddTrigger(trigger: OTTrigger)

        val withIndex: Iterable<IndexedValue<OTTrigger>>
    }

    var triggerAdapter: TriggerAdapter? = null
        set(value) {
            if (field !== value) {
                field = value
                refresh()
            }
        }

    abstract val triggerActionTypeName: Int

    abstract val triggerActionType: Int

    protected abstract val emptyMessageId: Int

    private lateinit var adapter: Adapter

    private lateinit var listView: FallbackRecyclerView

    private lateinit var newTriggerButton: FloatingActionButton

    //private var expandedTriggerPosition: Int = -1
    //private var expandedViewHolder: ATriggerViewHolder<out OTTrigger>? = null

    private val subscriptions = CompositeSubscription()

    private val triggerTypeDialog: AlertDialog by lazy {
        NewTriggerTypeSelectionDialogHelper.builder(parent.context, triggerActionTypeName) {
            type ->
            println("trigger type selected - $type")
            /*
            val newTrigger = triggerAdapter?.makeNewTrfiggerInstance(type)
            if (newTrigger != null) {
                appendNewTrigger(newTrigger)
            }*/
            parent.startActivityForResult(TriggerDetailActivity.makeNewTriggerIntent(parent.context, type, triggerActionType, hideTrackerAssignmentInterface()), DETAIL_REQUEST_CODE)
            triggerTypeDialog.dismiss()
        }.create()
    }

    open fun hideTrackerAssignmentInterface(): Boolean {
        return false
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
        //expandedTriggerPosition = savedInstanceState?.getInt(STATE_EXPANDED_POSITION, -1) ?: -1
        listView.adapter = adapter

        newTriggerButton = rootView.findViewById(R.id.ui_button_new_trigger) as FloatingActionButton

        newTriggerButton.setOnClickListener {
            onNewTriggerButtonClicked()
        }

        return rootView
    }

    fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
/*
        val activity = parent.activity
        if (activity is OTActivity) {
            subscriptions.add(
                    activity.signedInUserObservable.subscribe {
                        user ->
                        this.user = user
                        adapter.notifyDataSetChanged()
                    }
            )
        }*/

        val activity = parent.activity
        if (activity is OTActivity) {
            subscriptions.add(
                    activity.signedInUserObservable.subscribe {
                        user ->
                        subscriptions.add(
                                user.triggerManager.triggerAdded.subscribe {
                                    trigger ->
                                    println("trigger added")
                                    refresh()
                                }
                        )

                        subscriptions.add(
                                user.triggerManager.triggerRemoved.subscribe {
                                    trigger ->
                                    println("trigger removed")
                                    refresh()
                                }
                        )
                    }
            )
        }
    }

    fun refresh() {
        adapter.notifyDataSetChanged()
    }

    fun onDestroyView() {
        subscriptions.clear()
        adapter.unSubscribeAll()
    }

    fun onSaveInstanceState(outState: Bundle) {
        // outState.putInt(STATE_EXPANDED_POSITION, expandedTriggerPosition)
    }

    fun setFloatingButtonColor(color: Int) {
        newTriggerButton.colorNormal = color
        val hsv = floatArrayOf(0f, 0f, 0f)
        Color.colorToHSV(color, hsv)
        hsv[2] *= 0.8f
        newTriggerButton.colorPressed = Color.HSVToColor(hsv)
    }

    protected fun appendNewTrigger(trigger: OTTrigger) {
        adapter.notifyItemChanged(adapter.itemCount - 1)
        triggerAdapter?.onAddTrigger(trigger)

        adapter.notifyItemInserted(adapter.itemCount - 1)
        listView.smoothScrollToPosition(adapter.itemCount - 1)
    }

    protected open fun onNewTriggerButtonClicked() {
        triggerTypeDialog.show()
    }

    protected open fun onTriggerEditRequested(trigger: OTTrigger) {
        parent.startActivityForResult(
                TriggerDetailActivity.makeEditTriggerIntent(parent.context, trigger, hideTrackerAssignmentInterface()),
                DETAIL_REQUEST_CODE
        )
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == DETAIL_REQUEST_CODE) {
            println("Received Trigger detail activity result")
            if (resultCode == AppCompatActivity.RESULT_OK) {
                if (data != null && data.hasExtra(TriggerDetailActivity.INTENT_EXTRA_TRIGGER_DATA)) {
                    val triggerPojo = data.getSerializableExtra(TriggerDetailActivity.INTENT_EXTRA_TRIGGER_DATA)
                    if (triggerPojo is FirebaseHelper.TriggerPOJO) {
                        OTApplication.app.currentUserObservable.subscribe {
                            user ->
                            val newTrigger = OTTrigger.makeInstance(FirebaseHelper.generateNewKey(FirebaseHelper.CHILD_NAME_TRIGGERS), user, triggerPojo)
                            appendNewTrigger(newTrigger)
                            EventLoggingManager.logTriggerChangeEvent(EventLoggingManager.EVENT_NAME_CHANGE_TRIGGER_ADD, newTrigger.objectId, newTrigger.typeId, newTrigger.action)
                        }
                    }
                }
            }
        }
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
            return triggerAdapter?.getTriggerAt(position)?.typeId ?: -1
        }

        override fun onBindViewHolder(holder: ATriggerViewHolder<out OTTrigger>, position: Int) {
            if (triggerAdapter != null) {
                holder.bind(triggerAdapter!!.getTriggerAt(position))
                /*
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
                }*/
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
            return triggerAdapter?.triggerCount() ?: 0
        }


        override fun onTriggerEdited(position: Int) {

        }

        override fun onTriggerEditRequested(position: Int, viewHolder: ATriggerViewHolder<out OTTrigger>) {
            this@ATriggerListFragmentCore.onTriggerEditRequested(triggerAdapter!!.getTriggerAt(position))
        }

        override fun onTriggerRemove(position: Int) {

            if (triggerAdapter != null) {
                /*
                if (expandedTriggerPosition == position) {
                    val lastExpandedIndex = expandedTriggerPosition
                    expandedTriggerPosition = -1
                    expandedViewHolder = null
                    for (triggerEntry in triggerAdapter!!.withIndex) {
                        if (triggerEntry.index != lastExpandedIndex) {
                            this.notifyItemChanged(triggerEntry.index)
                        }
                    }
                }*/

                val trigger = triggerAdapter!!.getTriggerAt(position)
                triggerAdapter?.onRemoveTrigger(trigger)
                this.notifyItemRemoved(position)
                newTriggerButton.visibility = View.VISIBLE

                EventLoggingManager.logTriggerChangeEvent(EventLoggingManager.EVENT_NAME_CHANGE_TRIGGER_REMOVE, trigger.objectId, trigger.typeId, trigger.action)
            }
        }

        /*
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
        }*/
    }

}