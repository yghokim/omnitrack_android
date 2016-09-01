package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.app.ActionBar
import android.content.Context
import android.graphics.Color
import android.support.v7.widget.AppCompatImageButton
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.RecyclerView
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.FrameLayout
import android.widget.Switch
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.components.common.LockableFrameLayout
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import java.util.*

/**
 * Created by younghokim on 16. 8. 24..
 */
abstract class ATriggerViewHolder<T : OTTrigger>(parent: ViewGroup, val listener: ITriggerControlListener, context: Context) :
        RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.trigger_list_element, parent, false))
        , View.OnClickListener {

    interface ITriggerControlListener {
        fun onTriggerRemove(position: Int)
        fun onTriggerEdited(position: Int)
        fun onTriggerExpandRequested(position: Int)
        fun onTriggerCollapse(position: Int)
    }

    private var isFirstBinding = true

    protected lateinit var trigger: T
        private set

    var isExpanded: Boolean = true
        private set

    private val triggerSwitch: Switch

    private val removeButton: AppCompatImageButton
    private val expandToggleButton: AppCompatImageButton

    private val typeIconView: AppCompatImageView
    private val typeDescriptionView: TextView
    private val typeWappen: View

    private val configSummaryView: TextView
    private val expandedView: ViewGroup
    private val controlPanelContainer: ViewGroup
    private val collapsedView: ViewGroup
    private val headerViewContainer: ViewGroup

    private val applyButtonGroup: ViewGroup
    private val applyButton: View
    private val cancelButton: View

    private var attachedTrackerListView: View? = null
    private var attachedTrackerList: ViewGroup? = null
    private var attachedTrackerNoTrackerFallbackView: View? = null

    private val attachedTrackerListStub: ViewStub

    private val trackerAssignPanelStub: ViewStub
    private var trackerAssignPanelContainer: View? = null
    private var trackerAssignPanel: TrackerAssignPanel? = null

    private val bottomBar: LockableFrameLayout

    private val errorMessages: ArrayList<String>

    private val onTriggerSwitchTurned: ((sender: Any, isOn: Boolean) -> Unit) = {
        sender, isOn ->
        applyTriggerStateToView()
    }

    private val onTriggerFired: ((Any, Long) -> Unit) = {
        sender, triggeredTime ->
        applyTriggerStateToView()
    }


    init {

        errorMessages = ArrayList<String>()

        itemView.setOnClickListener(this)

        triggerSwitch = itemView.findViewById(R.id.ui_trigger_switch) as Switch
        triggerSwitch.setOnClickListener(this)

        removeButton = itemView.findViewById(R.id.ui_button_remove) as AppCompatImageButton
        removeButton.setOnClickListener(this)

        expandToggleButton = itemView.findViewById(R.id.ui_button_expand_toggle) as AppCompatImageButton
        expandToggleButton.setOnClickListener(this)

        bottomBar = itemView.findViewById(R.id.ui_bottom_bar) as LockableFrameLayout
        bottomBar.setOnClickListener(this)

        typeIconView = itemView.findViewById(R.id.ui_type_icon) as AppCompatImageView
        typeDescriptionView = itemView.findViewById(R.id.ui_type_description) as TextView
        typeWappen = itemView.findViewById(R.id.ui_wappen_type)

        configSummaryView = itemView.findViewById(R.id.ui_config_summary) as TextView
        expandedView = itemView.findViewById(R.id.ui_expanded_view) as ViewGroup
        controlPanelContainer = itemView.findViewById(R.id.ui_control_panel) as ViewGroup
        collapsedView = itemView.findViewById(R.id.ui_collapsed_view) as ViewGroup

        headerViewContainer = itemView.findViewById(R.id.ui_header_view_container) as ViewGroup

        applyButtonGroup = itemView.findViewById(R.id.ui_apply_button_group) as ViewGroup
        applyButton = itemView.findViewById(R.id.ui_button_apply)
        applyButton.setOnClickListener(this)

        cancelButton = itemView.findViewById(R.id.ui_button_cancel)
        cancelButton.setOnClickListener(this)

        attachedTrackerListStub = itemView.findViewById(R.id.ui_attached_tracker_list_stub) as ViewStub

        trackerAssignPanelStub = itemView.findViewById(R.id.ui_tracker_assign_panel_stub) as ViewStub

        setIsExpanded(false, false)
    }

    fun setIsExpanded(isExpanded: Boolean, animate: Boolean) {
        if (this.isExpanded != isExpanded) {
            this.isExpanded = isExpanded
            if (animate) {
                TransitionManager.beginDelayedTransition(itemView as ViewGroup)
            }

            if (isExpanded) {
                removeButton.visibility = View.VISIBLE
                configSummaryView.visibility = View.INVISIBLE
                expandToggleButton.setImageResource(R.drawable.up_dark)
                collapsedView.visibility = View.GONE
                if (controlPanelContainer.childCount == 0) {
                    val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT)
                    controlPanelContainer.addView(initExpandedViewContent(), lp)
                }
                updateExpandedViewContent(controlPanelContainer.getChildAt(0), trigger)
                expandedView.visibility = View.VISIBLE

                bottomBar.locked = false
                bottomBar.setBackgroundColor(itemView.resources.getColor(R.color.colorSecondary, null))
                removeButton.setColorFilter(Color.WHITE)
                applyButtonGroup.visibility = View.VISIBLE
                expandToggleButton.visibility = View.INVISIBLE

            } else {
                removeButton.visibility = View.INVISIBLE
                configSummaryView.visibility = View.VISIBLE
                expandToggleButton.setImageResource(R.drawable.down_dark)
                expandedView.visibility = View.GONE
                collapsedView.visibility = View.VISIBLE

                bottomBar.locked = true
                bottomBar.setBackgroundColor(itemView.resources.getColor(R.color.editTextFormBackground, null))
                removeButton.setColorFilter(itemView.resources.getColor(R.color.buttonIconColorDark, null))
                applyButtonGroup.visibility = View.INVISIBLE
                expandToggleButton.visibility = View.VISIBLE
            }
        }
    }

    fun bind(trigger: OTTrigger) {
        if (!isFirstBinding) {
            this.trigger.switchTurned -= onTriggerSwitchTurned
            this.trigger.fired -= onTriggerFired
        } else {
            isFirstBinding = false
        }

        this.trigger = trigger as T


        this.trigger.switchTurned += onTriggerSwitchTurned
        this.trigger.fired += onTriggerFired


        //attached tracker list
        if (this.trigger.action == OTTrigger.ACTION_BACKGROUND_LOGGING) {
            if (attachedTrackerListView == null) {
                attachedTrackerListView = attachedTrackerListStub.inflate()
                attachedTrackerList = attachedTrackerListView?.findViewById(R.id.ui_attached_tracker_list) as ViewGroup
                attachedTrackerNoTrackerFallbackView = attachedTrackerListView?.findViewById(R.id.ui_attached_tracker_list_empty_fallback)
            } else {
                attachedTrackerListView?.visibility = View.VISIBLE
            }

            if (trackerAssignPanelContainer == null) {
                trackerAssignPanelContainer = trackerAssignPanelStub.inflate()
                trackerAssignPanel = trackerAssignPanelContainer?.findViewById(R.id.ui_tracker_assign_list) as TrackerAssignPanel
                trackerAssignPanel?.init(trigger.trackers)
            } else {
                trackerAssignPanelContainer?.visibility = View.VISIBLE
            }

        } else {
            attachedTrackerListView?.visibility = View.GONE
            trackerAssignPanelContainer?.visibility = View.GONE
        }


        applyTriggerStateToView()
    }

    protected fun applyTriggerStateToView() {
        triggerSwitch.isChecked = trigger.isOn

        configSummaryView.text = getConfigSummary(trigger)

        typeIconView.setImageResource(trigger.configIconId)
        typeDescriptionView.setText(trigger.configTitleId)

        if (headerViewContainer.childCount > 0) {
            val headerView = getHeaderView(headerViewContainer.getChildAt(0), trigger)
            if (headerView !== headerViewContainer.getChildAt(0)) {
                headerViewContainer.removeAllViews()
                headerViewContainer.addView(headerView)
            }
        } else {
            headerViewContainer.addView(getHeaderView(null, trigger))
        }

        val listView = attachedTrackerList
        if (listView != null) {
            if (trigger.trackers.isEmpty()) {
                attachedTrackerNoTrackerFallbackView?.visibility = View.VISIBLE
                listView.visibility = View.GONE
            } else {
                attachedTrackerNoTrackerFallbackView?.visibility = View.INVISIBLE
                listView.visibility = View.VISIBLE
                //sync
                val differ = trigger.trackers.size - listView.childCount
                if (differ > 0) {
                    for (i in 1..differ) {
                        val newView = listView.inflateContent(R.layout.layout_attached_tracker_list_element, false)
                        newView.tag = AttachedTrackerViewHolder(newView)
                        listView.addView(newView, 0)
                    }
                } else if (differ < 0) {
                    for (i in 1..-differ) {
                        listView.removeViewAt(listView.childCount - 1)
                    }
                }

                for (tracker in trigger.trackers.withIndex()) {
                    val vh = (listView.getChildAt(tracker.index).tag as AttachedTrackerViewHolder)
                    vh.textView.text = tracker.value.name
                    vh.colorBar.setBackgroundColor(tracker.value.color)
                }
            }
        }
    }


    protected abstract fun getHeaderView(current: View?, trigger: T): View

    protected abstract fun initExpandedViewContent(): View

    protected abstract fun updateExpandedViewContent(expandedView: View, trigger: T): Unit
    abstract fun updateTriggerWithViewSettings(expandedView: View, trigger: T): Unit

    protected abstract fun getConfigSummary(trigger: T): CharSequence

    protected abstract fun validateExpandedViewInputs(expandedView: View, errorMessagesOut: MutableList<String>): Boolean

    override fun onClick(view: View?) {

        if (view === removeButton) {
            listener.onTriggerRemove(adapterPosition)
        } else if (view === expandToggleButton) {
            listener.onTriggerCollapse(adapterPosition)
        } else if (view === triggerSwitch) {
            trigger.isOn = triggerSwitch.isChecked
        } else if (view === bottomBar || view === itemView) {
            if (!isExpanded) {
                listener.onTriggerExpandRequested(adapterPosition)
            }
        } else if (view === applyButton) {
            if (validateExpandedViewInputs(controlPanelContainer.getChildAt(0), errorMessages)) {
                updateTriggerWithViewSettings(controlPanelContainer.getChildAt(0), trigger)

                if (trackerAssignPanel != null) {
                    for (trackerId in trackerAssignPanel!!.trackerIds) {
                        trigger.addTracker(trackerId)
                    }

                    for (tracker in trigger.trackers) {
                        if (!trackerAssignPanel!!.trackerIds.contains(tracker.objectId)) {
                            trigger.removeTracker(tracker)
                        }
                    }

                }

                listener.onTriggerCollapse(adapterPosition)
            } else {
                //validation failed
                DialogHelper.makeSimpleAlertBuilder(itemView.context, errorMessages.joinToString("\n")).show()
            }
        } else if (view === cancelButton) {
            listener.onTriggerCollapse(adapterPosition)
        }
    }

    open class AttachedTrackerViewHolder(val view: View) {
        val textView: TextView
        val colorBar: View

        init {
            textView = view.findViewById(R.id.text) as TextView
            colorBar = view.findViewById(R.id.color_bar)
        }
    }



}