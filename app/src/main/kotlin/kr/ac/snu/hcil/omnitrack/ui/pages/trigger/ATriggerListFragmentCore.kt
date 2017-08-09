package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.util.DiffUtil
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
import kr.ac.snu.hcil.omnitrack.core.database.DatabaseManager
import kr.ac.snu.hcil.omnitrack.core.database.EventLoggingManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.FallbackRecyclerView
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.DrawableListBottomSpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.DataTriggerViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.TimeTriggerViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.TriggerListViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.TriggerViewModel
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
/*
    interface TriggerAdapter {
        fun getTriggerAt(position: Int): OTTrigger
        fun triggerCount(): Int

        fun makeNewTriggerInstance(type: Int): OTTrigger

        //user?.triggerManager?.removeTrigger(trigger)
        fun onRemoveTrigger(trigger: OTTrigger)

        //user?.triggerManager?.putNewTrigger(trigger)
        fun onAddTrigger(trigger: OTTrigger)

        val withIndex: Iterable<IndexedValue<OTTrigger>>
    }*/

    private lateinit var viewModel: TriggerListViewModel

    abstract val triggerActionTypeName: Int

    abstract val triggerActionType: Int

    protected abstract val emptyMessageId: Int

    protected abstract val triggerFilter: (OTTrigger) -> Boolean

    private lateinit var adapter: Adapter

    private lateinit var listView: FallbackRecyclerView

    private lateinit var newTriggerButton: FloatingActionButton

    private var newlyAddedTriggerId: String? = null

    private val currentTriggerViewModels = ArrayList<TriggerViewModel<OTTrigger>>()

    //private var expandedTriggerPosition: Int = -1
    //private var expandedViewHolder: ATriggerViewHolder<out OTTrigger>? = null

    private val createViewSubscriptions = CompositeSubscription()
    private val creationSubscriptions = CompositeSubscription()

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

    fun onCreate(savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(parent).get(TriggerListViewModel::class.java)
        viewModel.triggerFilter = this.triggerFilter

        val activity = parent.activity
        if (activity is OTActivity) {
            creationSubscriptions.add(
                    activity.signedInUserObservable.subscribe {
                        user ->
                        viewModel.user = user

                        creationSubscriptions.add(
                                viewModel.triggerViewModelListSubject.subscribe {
                                    list ->
                                    val diffResult = DiffUtil.calculateDiff(TriggerListViewModel.TriggerViewModelListDiffUtilCallback(currentTriggerViewModels, list))
                                    currentTriggerViewModels.clear()
                                    currentTriggerViewModels.addAll(list)
                                    diffResult.dispatchUpdatesTo(adapter)
                                }
                        )
                    }
            )
        }
    }

    fun onDestroy() {
        creationSubscriptions.clear()
    }

    fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, @Suppress("UNUSED_PARAMETER") savedInstanceState: Bundle?): View {
        val rootView = inflater!!.inflate(R.layout.fragment_tracker_detail_triggers, container, false)

        listView = rootView.findViewById(R.id.ui_trigger_list)
        listView.emptyView = rootView.findViewById<TextView>(R.id.ui_empty_list_message).apply {
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

        newTriggerButton = rootView.findViewById(R.id.ui_button_new_trigger)

        setFloatingButtonColor(ContextCompat.getColor(rootView.context, R.color.colorPointed))

        newTriggerButton.setOnClickListener {
            onNewTriggerButtonClicked()
        }

        return rootView
    }

    fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
/*
        val activity = parent.activity
        if (activity is OTActivity) {
            createViewSubscriptions.add(
                    activity.signedInUserObservable.subscribe {
                        user ->
                        this.user = user
                        adapter.notifyDataSetChanged()
                    }
            )
        }*/

        val activity = parent.activity
        if (activity is OTActivity) {
            createViewSubscriptions.add(
                    activity.signedInUserObservable.subscribe {
                        user ->
                        createViewSubscriptions.add(
                                user.triggerManager.triggerAdded.subscribe {
                                    trigger ->
                                    println("trigger added")
                                    refresh()
                                }
                        )

                        createViewSubscriptions.add(
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
        createViewSubscriptions.clear()
        adapter.unSubscribeAll()
    }

    fun onSaveInstanceState(outState: Bundle) {
        // outState.putInt(STATE_EXPANDED_POSITION, expandedTriggerPosition)
    }

    fun setFloatingButtonColor(color: Int) {
        newTriggerButton.backgroundTintList = ColorStateList.valueOf(color)
        val hsv = floatArrayOf(0f, 0f, 0f)
        Color.colorToHSV(color, hsv)
        hsv[2] *= 0.8f
        newTriggerButton.rippleColor = Color.HSVToColor(hsv)
    }

    protected fun appendNewTrigger(trigger: OTTrigger) {
        viewModel.addTrigger(trigger)
        listView.smoothScrollToPosition(adapter.itemCount - 1)
    }

    protected open fun onNewTriggerButtonClicked() {
        triggerTypeDialog.show()
    }

    protected open fun onTriggerEditRequested(triggerId: String) {
        parent.startActivityForResult(
                TriggerDetailActivity.makeEditTriggerIntent(parent.context, triggerId, hideTrackerAssignmentInterface()),
                DETAIL_REQUEST_CODE
        )
    }

    protected open fun postProcessNewlyAddedTrigger(newTrigger: OTTrigger) {

    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == DETAIL_REQUEST_CODE) {
            println("Received Trigger detail activity result")
            if (resultCode == AppCompatActivity.RESULT_OK) {
                if (data != null && data.hasExtra(TriggerDetailActivity.INTENT_EXTRA_TRIGGER_DATA)) {
                    val triggerPojo = data.getSerializableExtra(TriggerDetailActivity.INTENT_EXTRA_TRIGGER_DATA)
                    if (triggerPojo is DatabaseManager.TriggerPOJO) {
                        OTApplication.app.currentUserObservable.subscribe {
                            user ->
                            val newTrigger = OTTrigger.makeInstance(DatabaseManager.generateNewKey(DatabaseManager.CHILD_NAME_TRIGGERS), user, triggerPojo)
                            newlyAddedTriggerId = newTrigger.objectId
                            postProcessNewlyAddedTrigger(newTrigger)
                            appendNewTrigger(newTrigger)
                            EventLoggingManager.logTriggerChangeEvent(EventLoggingManager.EVENT_NAME_CHANGE_TRIGGER_ADD, newTrigger.objectId, newTrigger.typeId, newTrigger.action)
                        }
                    }
                }
            }
        }
    }

    inner class Adapter : RecyclerView.Adapter<ATriggerViewHolder<*>>(), ATriggerViewHolder.ITriggerControlListener {

        private val viewHolders = ArrayList<ATriggerViewHolder<*>>()

        fun unSubscribeAll() {
            for (viewHolder in viewHolders) {
                viewHolder.unSubscribeAll()
            }
            viewHolders.clear()
        }

        override fun getItemViewType(position: Int): Int {
            return currentTriggerViewModels[position].triggerType.value ?: -1
        }

        override fun onBindViewHolder(holder: ATriggerViewHolder<*>, position: Int) {
            val viewModel = currentTriggerViewModels[position]
            if (holder is TimeTriggerViewHolder && viewModel is TimeTriggerViewModel) {
                holder.bind(viewModel, viewModel.triggerId.value == newlyAddedTriggerId)
            } else if (holder is EventTriggerViewHolder && viewModel is DataTriggerViewModel) {
                holder.bind(viewModel, viewModel.triggerId.value == newlyAddedTriggerId)
            }
        }

        override fun onCreateViewHolder(parentView: ViewGroup, viewType: Int): ATriggerViewHolder<*> {

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
            return currentTriggerViewModels.size
        }


        override fun onTriggerEdited(position: Int) {

        }

        override fun onTriggerEditRequested(position: Int, viewHolder: ATriggerViewHolder<*>) {
            this@ATriggerListFragmentCore.onTriggerEditRequested(currentTriggerViewModels.get(position).triggerId.value)
        }

        override fun onTriggerRemove(position: Int) {
            val triggerViewModel = currentTriggerViewModels[position]
            viewModel.removeTrigger(triggerViewModel)
            newTriggerButton.visibility = View.VISIBLE

            EventLoggingManager.logTriggerChangeEvent(EventLoggingManager.EVENT_NAME_CHANGE_TRIGGER_REMOVE, triggerViewModel.triggerId.value, triggerViewModel.triggerType.value, triggerViewModel.triggerAction.value)
        }
    }
}