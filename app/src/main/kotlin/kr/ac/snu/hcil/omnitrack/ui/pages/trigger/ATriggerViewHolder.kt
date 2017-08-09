package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.animation.ValueAnimator
import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatImageButton
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.TextView
import butterknife.bindView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.database.EventLoggingManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.components.common.LockableFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.common.SwipelessSwitchCompat
import kr.ac.snu.hcil.omnitrack.ui.components.common.ValidatedSwitch
import kr.ac.snu.hcil.omnitrack.ui.components.tutorial.TutorialManager
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.TriggerViewModel
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.getActivity
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription

/**
 * Created by Young-Ho Kim on 16. 8. 24
 */
abstract class ATriggerViewHolder<T>(parent: ViewGroup, val listener: ITriggerControlListener, context: Context) :
        RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.trigger_list_element, parent, false))
        , View.OnClickListener, ValidatedSwitch.IValidationListener where T : TriggerViewModel<OTTrigger> {

    interface ITriggerControlListener {
        fun onTriggerRemove(position: Int)
        fun onTriggerEdited(position: Int)
        fun onTriggerEditRequested(position: Int, viewHolder: ATriggerViewHolder<out TriggerViewModel<OTTrigger>>)
        //fun onTriggerExpandRequested(position: Int, viewHolder: ATriggerViewHolder<out OTTrigger>)
        //fun onTriggerCollapse(position: Int, viewHolder: ATriggerViewHolder<out OTTrigger>)
    }

    /*
    var isExpanded: Boolean = true
        private set*/

    private val triggerSwitch: ValidatedSwitch by bindView(R.id.ui_trigger_switch)

    private val removeButton: AppCompatImageButton by bindView(R.id.ui_button_remove)
    //private val expandToggleButton: AppCompatImageButton by bindView(R.id.ui_button_expand_toggle)

    private val typeIconView: AppCompatImageView by bindView(R.id.ui_type_icon)
    private val typeDescriptionView: TextView by bindView(R.id.ui_type_description)
    private val typeWappen: View by bindView(R.id.ui_wappen_type)

    //private val toggleViewContainer: ViewGroup by bindView(R.id.ui_toggle_view_container)

    private val configSummaryView: TextView by bindView(R.id.ui_config_summary)
    //private val expandedView: ViewGroup by bindView(R.id.ui_expanded_view)
    //private val controlPanelContainer: ViewGroup by bindView(R.id.ui_control_panel)
    //private val collapsedView: ViewGroup by bindView(R.id.ui_collapsed_view)
    protected val headerViewContainer: ViewGroup by bindView(R.id.ui_header_view_container)

    private var currentHeaderView: View? = null

    protected val headerViewSubscriptions = CompositeSubscription()

    //private val applyButtonGroup: ViewGroup by bindView(R.id.ui_apply_button_group)
    //private val applyButton: View by bindView(R.id.ui_button_apply)
    //private val cancelButton: View by bindView(R.id.ui_button_cancel)

    private var attachedTrackerListView: View? = null
    private var attachedTrackerList: ViewGroup? = null
    private var attachedTrackerNoTrackerFallbackView: View? = null

    private var currentAlertAnimation: ValueAnimator? = null

    private val attachedTrackerListStub: ViewStub by bindView(R.id.ui_attached_tracker_list_stub)

    /*
    private val trackerAssignPanelStub: ViewStub by bindView(R.id.ui_tracker_assign_panel_stub)
    private var trackerAssignPanelContainer: View? = null
    private var trackerAssignPanel: TrackerAssignPanel? = null*/

    private val bottomBar: LockableFrameLayout by bindView(R.id.ui_bottom_bar)

    private val subscriptions = CompositeSubscription()

    //private var collapsedHeight: Int = 0
    //private var expandedHeight: Int = 0

    private val triggerOnValidator: () -> Boolean = {
        validateTriggerSwitchOn()
    }

    protected open fun validateTriggerSwitchOn(): Boolean {
        /*
        return if (trackerAssignPanelContainer != null) {
            trigger.trackers.isNotEmpty()
        } else true*/
        return viewModel?.currentAttachedTrackers?.isNotEmpty() ?: false
    }

    private var viewModel: T? = null

    init {

        triggerSwitch.switchOnValidator = triggerOnValidator

        attachListener()

        //setIsExpanded(false, false)
    }

    private fun attachListener() {
        itemView.setOnClickListener(this)

        triggerSwitch.setOnClickListener(this)
        triggerSwitch.addValidationListener(this)

        removeButton.setOnClickListener(this)
    }

    fun unSubscribeAll() {
        subscriptions.clear()
        headerViewSubscriptions.clear()
        currentHeaderView = null
        headerViewContainer.removeAllViewsInLayout()
        viewModel = null
    }


    fun bind(viewModel: T, newCreated: Boolean) {
        unSubscribeAll()

        this.viewModel = viewModel

        currentAlertAnimation?.cancel()
        currentAlertAnimation = null

        /*
            subscriptions.add(
                    fired.subscribeOn(AndroidSchedulers.mainThread()).subscribe {
                        applyTriggerStateToView()
                    }
            )*/

        //attached tracker list
        subscriptions.add(
                viewModel.triggerAction.subscribe {
                    action ->
                    if (action == OTTrigger.ACTION_BACKGROUND_LOGGING) {
                        if (attachedTrackerListView == null) {
                            attachedTrackerListView = attachedTrackerListStub.inflate()
                            attachedTrackerList = attachedTrackerListView?.findViewById(R.id.ui_attached_tracker_list)
                            attachedTrackerNoTrackerFallbackView = attachedTrackerListView?.findViewById(R.id.ui_attached_tracker_list_empty_fallback)
                        } else {
                            attachedTrackerListView?.visibility = View.VISIBLE
                    }

                    } else {
                        attachedTrackerListView?.visibility = View.GONE
                        //trackerAssignPanelContainer?.visibility = View.GONE
                    }
                }
        )

        subscriptions.add(
                viewModel.triggerSwitch.subscribeOn(AndroidSchedulers.mainThread()).subscribe {
                    isOn ->
                    triggerSwitch.isChecked = isOn
                }
        )

        subscriptions.add(
                viewModel.triggerConfigIconResId.subscribe {
                    iconId ->
                    if (iconId != null)
                        typeIconView.setImageResource(iconId)
                }
        )

        subscriptions.add(
                viewModel.triggerDescResId.subscribe {
                    nameId ->
                    typeDescriptionView.setText(nameId)
                }
        )

        subscriptions.add(
                viewModel.triggerConfigSummary.subscribe {
                    message ->
                    configSummaryView.text = message
                }
        )


        /*
        if (headerViewContainer.childCount > 0) {
            val headerView = getHeaderView(headerViewContainer.getChildAt(0), trigger)
            if (headerView !== headerViewContainer.getChildAt(0)) {
                headerViewContainer.removeAllViews()
                headerViewContainer.addView(headerView)
            }
        } else {
            headerViewContainer.addView(getHeaderView(null, trigger))
        }*/

        refreshHeaderView(getHeaderView(currentHeaderView, viewModel))

        subscriptions.add(
                viewModel.attachedTriggers.subscribe {
                    list ->
                    refreshAttachedTrackerList(list)
                }
        )


        if (newCreated) {
            val activity = itemView.getActivity()
            if (activity != null) {
                TutorialManager.checkAndShowTargetPrompt("has_created_trigger", true, activity, triggerSwitch,
                        R.string.msg_tutorial_new_trigger_switch_primary,
                        R.string.msg_tutorial_new_trigger_switch_secondary,
                        ContextCompat.getColor(itemView.context, R.color.colorPointed))
            }
        }
    }

    protected abstract fun getHeaderView(current: View?, viewModel: T): View

    protected fun refreshHeaderView(headerView: View) {
        if (currentHeaderView !== headerView) {
            headerViewContainer.removeAllViewsInLayout()
            headerViewContainer.addView(headerView)
            currentHeaderView = headerView
        }
    }

    protected open fun refreshAttachedTrackerList(trackers: List<OTTracker>) {
        val listView = attachedTrackerList
        if (listView != null) {
            if (trackers.isEmpty()) {
                attachedTrackerNoTrackerFallbackView?.visibility = View.VISIBLE
                listView.visibility = View.GONE
            } else {
                attachedTrackerNoTrackerFallbackView?.visibility = View.INVISIBLE
                listView.visibility = View.VISIBLE
                //sync
                val differ = trackers.size - listView.childCount
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

                for (tracker in trackers.withIndex()) {
                    val vh = (listView.getChildAt(tracker.index).tag as AttachedTrackerViewHolder)
                    vh.textView.text = tracker.value.name
                    vh.colorBar.setBackgroundColor(tracker.value.color)
                }
            }
        }
    }

    //protected abstract fun initExpandedViewContent(): View

    //protected abstract fun updateExpandedViewContent(expandedView: View, trigger: T): Unit
    //abstract fun updateTriggerWithViewSettings(expandedView: View, trigger: T): Unit

    //protected abstract fun validateExpandedViewInputs(expandedView: View, errorMessagesOut: MutableList<String>): Boolean

    override fun onValidationFailed(switch: SwipelessSwitchCompat, on: Boolean) {
        if (switch === triggerSwitch && on) {
            onSwitchOnValidationFailed()
        }
    }

    protected open fun getViewsForSwitchValidationFailedAlert(): Array<View>? {
        return if (attachedTrackerNoTrackerFallbackView != null) {
            arrayOf(attachedTrackerNoTrackerFallbackView!!)
        } else null
    }

    protected fun onSwitchOnValidationFailed() {
        val alertViews = getViewsForSwitchValidationFailedAlert()
        if (alertViews != null) {
            currentAlertAnimation?.cancel()

            currentAlertAnimation = InterfaceHelper.alertBackground(alertViews)
            currentAlertAnimation?.start()
        }
    }

    override fun onValidationSucceeded(switch: SwipelessSwitchCompat, on: Boolean) {
    }

    override fun onClick(view: View?) {

        println("clicked : ${view?.javaClass}")

        if (view === removeButton) {
            DialogHelper.makeNegativePhrasedYesNoDialogBuilder(itemView.context, "OmniTrack", itemView.context.resources.getString(R.string.msg_trigger_remove_confirm), R.string.msg_remove, onYes= {
                listener.onTriggerRemove(adapterPosition)
            }).show()
        } else if (view === triggerSwitch) {
            viewModel?.let {
                viewModel ->
                println("trigger switch pressed")
                viewModel.setTriggerSwitch(triggerSwitch.isChecked)
                val eventParams = EventLoggingManager.makeTriggerChangeEventParams(viewModel.triggerId.value, viewModel.triggerType.value, viewModel.triggerAction.value)
                eventParams.putBoolean("switch", viewModel.triggerSwitch.value)
                EventLoggingManager.logEvent(EventLoggingManager.EVENT_NAME_CHANGE_TRIGGER_SWITCH, eventParams)
            }

        } else if (view === itemView) {
            listener.onTriggerEditRequested(adapterPosition, this)
        }
    }

    open class AttachedTrackerViewHolder(val view: View) {
        val textView: TextView = view.findViewById(R.id.text)
        val colorBar: View = view.findViewById(R.id.color_bar)
    }


}