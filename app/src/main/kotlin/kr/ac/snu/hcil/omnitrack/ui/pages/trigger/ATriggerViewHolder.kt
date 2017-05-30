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
import kr.ac.snu.hcil.omnitrack.core.database.EventLoggingManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.components.common.LockableFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.common.SwipelessSwitchCompat
import kr.ac.snu.hcil.omnitrack.ui.components.common.ValidatedSwitch
import kr.ac.snu.hcil.omnitrack.ui.components.tutorial.TutorialManager
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.getActivity
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription

/**
 * Created by Young-Ho Kim on 16. 8. 24
 */
abstract class ATriggerViewHolder<T : OTTrigger>(parent: ViewGroup, val listener: ITriggerControlListener, context: Context) :
        RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.trigger_list_element, parent, false))
        , View.OnClickListener, ValidatedSwitch.IValidationListener {

    interface ITriggerControlListener {
        fun onTriggerRemove(position: Int)
        fun onTriggerEdited(position: Int)
        fun onTriggerEditRequested(position: Int, viewHolder: ATriggerViewHolder<out OTTrigger>)
        //fun onTriggerExpandRequested(position: Int, viewHolder: ATriggerViewHolder<out OTTrigger>)
        //fun onTriggerCollapse(position: Int, viewHolder: ATriggerViewHolder<out OTTrigger>)
    }

    //private var isFirstBinding = true

    protected lateinit var trigger: T
        private set

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
        return trigger.trackers.isNotEmpty()
    }

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

        //expandToggleButton.setOnClickListener(this)

        //bottomBar.setOnClickListener(this)

        //applyButton.setOnClickListener(this)

        //cancelButton.setOnClickListener(this)
    }

    /*
    fun setIsExpanded(isExpanded: Boolean, animate: Boolean) {
        if (this.isExpanded != isExpanded) {

            currentAlertAnimation?.cancel()
            currentAlertAnimation = null

            if (!isFirstBinding)
                applyTriggerStateToView()

            this.isExpanded = isExpanded

            if (isExpanded) {

                removeButton.visibility = View.VISIBLE
                configSummaryView.visibility = View.INVISIBLE
                expandToggleButton.setImageResource(R.drawable.up_dark)
                if (controlPanelContainer.childCount == 0) {
                    val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT)
                    controlPanelContainer.addView(initExpandedViewContent(), lp)
                }


                if (trackerAssignPanel != null) {
                    trackerAssignPanel?.init(trigger.trackers)
                }
                updateExpandedViewContent(controlPanelContainer.getChildAt(0), trigger)


                bottomBar.locked = false
                bottomBar.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.colorSecondary))
                removeButton.setColorFilter(Color.WHITE)
                applyButtonGroup.visibility = View.VISIBLE
                expandToggleButton.visibility = View.INVISIBLE

                if (animate) {
                    measureCollapsedAndExpandedHeight()

                    val animator = ValueAnimator.ofFloat(0f, 1f)
                            .apply {
                                duration = 200
                                interpolator = DecelerateInterpolator()

                                addListener(object : Animator.AnimatorListener {
                                    override fun onAnimationCancel(p0: Animator?) {

                                    }

                                    override fun onAnimationEnd(p0: Animator?) {
                                        collapsedView.run {
                                            visibility = View.GONE
                                            alpha = 1f
                                        }

                                        toggleViewContainer.run {
                                            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                                            requestLayout()
                                        }
                                    }

                                    override fun onAnimationRepeat(p0: Animator?) {
                                    }

                                    override fun onAnimationStart(p0: Animator?) {
                                        expandedView.visibility = View.VISIBLE
                                        collapsedView.visibility = View.VISIBLE
                                    }

                                })

                                addUpdateListener {
                                    val ratio = animatedValue as Float
                                    collapsedView.alpha = 1 - ratio
                                    expandedView.alpha = ratio
                                    toggleViewContainer.run {
                                        layoutParams.height = (0.5f + collapsedHeight + (expandedHeight - collapsedHeight) * ratio).toInt()
                                        requestLayout()
                                    }
                                }

                            }
                    animator.start()
                } else {
                    toggleViewContainer.run {
                        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        requestLayout()
                    }
                    collapsedView.visibility = View.GONE
                    expandedView.visibility = View.VISIBLE

                }


            } else {

                removeButton.run {
                    visibility = View.INVISIBLE
                    setColorFilter(ContextCompat.getColor(itemView.context, R.color.buttonIconColorDark))
                }

                configSummaryView.visibility = View.VISIBLE

                expandToggleButton.run {
                    setImageResource(R.drawable.down_dark)
                    visibility = View.VISIBLE
                }

                bottomBar.run {
                    locked = true
                    setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.editTextFormBackground))
                }

                applyButtonGroup.visibility = View.INVISIBLE


                if (animate) {
                    measureCollapsedAndExpandedHeight()

                    val animator = ValueAnimator.ofFloat(1f, 0f)
                            .apply {
                                duration = 200
                                interpolator = AccelerateInterpolator()

                                addListener(object : Animator.AnimatorListener {
                                    override fun onAnimationCancel(p0: Animator?) {

                                    }

                                    override fun onAnimationEnd(p0: Animator?) {
                                        expandedView.run {
                                            visibility = View.GONE
                                            alpha = 1f
                                        }
                                        toggleViewContainer.run {
                                            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                                            requestLayout()
                                        }
                                    }

                                    override fun onAnimationRepeat(p0: Animator?) {
                                    }

                                    override fun onAnimationStart(p0: Animator?) {
                                        collapsedView.visibility = View.VISIBLE
                                        expandedView.visibility = View.VISIBLE
                                    }

                                })

                                addUpdateListener {
                                    val ratio = animatedValue as Float
                                    collapsedView.alpha = 1 - ratio
                                    expandedView.alpha = ratio
                                    toggleViewContainer.run {
                                        layoutParams.height = (0.5f + collapsedHeight + (expandedHeight - collapsedHeight) * ratio).toInt()
                                        requestLayout()
                                    }
                                }

                            }
                    animator.start()

                } else {
                    expandedView.visibility = View.GONE
                    collapsedView.visibility = View.VISIBLE
                }
            }
        }
    }*/
/*
    private fun measureCollapsedAndExpandedHeight() {
        collapsedView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        collapsedHeight = collapsedView.measuredHeight

        expandedView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        expandedHeight = expandedView.measuredHeight

        println("measured trigger view sizes: $collapsedHeight, $expandedHeight")
    }*/

    fun unSubscribeAll() {
        subscriptions.clear()
    }

    @Suppress("UNCHECKED_CAST")
    fun bind(trigger: OTTrigger, newCreated: Boolean) {

        subscriptions.clear()

        this.trigger = trigger as T

        currentAlertAnimation?.cancel()
        currentAlertAnimation = null

        this.trigger.run {
            subscriptions.add(
                    switchTurned.subscribeOn(AndroidSchedulers.mainThread()).subscribe {
                        applyTriggerStateToView()
                    }
            )

            subscriptions.add(
                    propertyChanged.subscribeOn(AndroidSchedulers.mainThread()).subscribe {
                        applyTriggerStateToView()
                    }
            )

            subscriptions.add(
                    fired.subscribeOn(AndroidSchedulers.mainThread()).subscribe {
                        applyTriggerStateToView()
                    }
            )

            subscriptions.add(
                    attachedTrackersChanged.subscribeOn(AndroidSchedulers.mainThread()).subscribe {
                        applyTriggerStateToView()
                    }
            )
        }

        //attached tracker list
        if (this.trigger.action == OTTrigger.ACTION_BACKGROUND_LOGGING) {
            if (attachedTrackerListView == null) {
                attachedTrackerListView = attachedTrackerListStub.inflate()
                attachedTrackerList = attachedTrackerListView?.findViewById(R.id.ui_attached_tracker_list) as ViewGroup
                attachedTrackerNoTrackerFallbackView = attachedTrackerListView?.findViewById(R.id.ui_attached_tracker_list_empty_fallback)
            } else {
                attachedTrackerListView?.visibility = View.VISIBLE
            }
/*
            if (trackerAssignPanelContainer == null) {
                trackerAssignPanelContainer = trackerAssignPanelStub.inflate()
                trackerAssignPanel = trackerAssignPanelContainer?.findViewById(R.id.ui_tracker_assign_list) as TrackerAssignPanel
                trackerAssignPanel?.init(trigger.trackers)
            } else {
                trackerAssignPanelContainer?.visibility = View.VISIBLE
            }*/

        } else {
            attachedTrackerListView?.visibility = View.GONE
            //trackerAssignPanelContainer?.visibility = View.GONE
        }


        applyTriggerStateToView()
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

    //protected abstract fun initExpandedViewContent(): View

    //protected abstract fun updateExpandedViewContent(expandedView: View, trigger: T): Unit
    //abstract fun updateTriggerWithViewSettings(expandedView: View, trigger: T): Unit

    protected abstract fun getConfigSummary(trigger: T): CharSequence

    //protected abstract fun validateExpandedViewInputs(expandedView: View, errorMessagesOut: MutableList<String>): Boolean

    override fun onValidationFailed(switch: SwipelessSwitchCompat, on: Boolean) {
        if (switch === triggerSwitch && on == true) {
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

        if (view === removeButton) {
            DialogHelper.makeNegativePhrasedYesNoDialogBuilder(itemView.context, "OmniTrack", itemView.context.resources.getString(R.string.msg_trigger_remove_confirm), R.string.msg_remove, onYes= {
                listener.onTriggerRemove(adapterPosition)
            }).show()
        }
        /*else if (view === expandToggleButton) {
            listener.onTriggerCollapse(adapterPosition, this)
        }*/ else if (view === triggerSwitch) {
            trigger.isOn = triggerSwitch.isChecked
            val eventParams = EventLoggingManager.makeTriggerChangeEventParams(trigger.objectId, trigger.typeId, trigger.action)
            eventParams.putBoolean("switch", trigger.isOn)
            EventLoggingManager.logEvent(EventLoggingManager.EVENT_NAME_CHANGE_TRIGGER_SWITCH, eventParams)

        } else if (view === itemView) {
            listener.onTriggerEditRequested(adapterPosition, this)
        }
        /* else if (view === bottomBar || view === itemView) {
            if (!isExpanded) {
                listener.onTriggerExpandRequested(adapterPosition, this)
            }
        } else if (view === applyButton) {
            if (validateExpandedViewInputs(controlPanelContainer.getChildAt(0), errorMessages)) {
                updateTriggerWithViewSettings(controlPanelContainer.getChildAt(0), trigger)

                if (trackerAssignPanel != null) {
                    for (trackerId in trackerAssignPanel!!.trackerIds) {
                        trigger.addTracker(trackerId)
                    }

                    var i = 0
                    while (i < trigger.trackers.size) {
                        val tracker = trigger.trackers[i]
                        if (!trackerAssignPanel!!.trackerIds.contains(tracker.objectId)) {
                            trigger.removeTracker(tracker)
                        } else {
                            i++
                        }
                    }
                }

                listener.onTriggerCollapse(adapterPosition, this)
            } else {
                //validation failed
                DialogHelper.makeSimpleAlertBuilder(itemView.context, errorMessages.joinToString("\n")).show()
            }
        } else if (view === cancelButton) {
            listener.onTriggerCollapse(adapterPosition, this)
        }*/
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