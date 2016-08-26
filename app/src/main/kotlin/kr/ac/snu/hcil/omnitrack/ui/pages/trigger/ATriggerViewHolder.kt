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
import android.widget.FrameLayout
import android.widget.Switch
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.components.common.LockableFrameLayout
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
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
    private val collapsedView: ViewGroup
    private val headerViewContainer: ViewGroup

    private val applyButtonGroup: ViewGroup
    private val applyButton: View
    private val cancelButton: View


    private val bottomBar: LockableFrameLayout

    private val errorMessages: ArrayList<String>

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
        collapsedView = itemView.findViewById(R.id.ui_collapsed_view) as ViewGroup

        headerViewContainer = itemView.findViewById(R.id.ui_header_view_container) as ViewGroup

        applyButtonGroup = itemView.findViewById(R.id.ui_apply_button_group) as ViewGroup
        applyButton = itemView.findViewById(R.id.ui_button_apply)
        applyButton.setOnClickListener(this)

        cancelButton = itemView.findViewById(R.id.ui_button_cancel)
        cancelButton.setOnClickListener(this)

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
                if (expandedView.childCount == 0) {
                    val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT)
                    expandedView.addView(initExpandedViewContent(), lp)
                }
                updateExpandedViewContent(expandedView.getChildAt(0), trigger)
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
        this.trigger = trigger as T

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
            if (validateExpandedViewInputs(expandedView.getChildAt(0), errorMessages)) {
                updateTriggerWithViewSettings(expandedView.getChildAt(0), trigger)
                listener.onTriggerCollapse(adapterPosition)
            } else {
                //validation failed
                DialogHelper.makeSimpleAlertBuilder(itemView.context, errorMessages.joinToString("\n")).show()
            }
        } else if (view === cancelButton) {
            listener.onTriggerCollapse(adapterPosition)
        }
    }

}