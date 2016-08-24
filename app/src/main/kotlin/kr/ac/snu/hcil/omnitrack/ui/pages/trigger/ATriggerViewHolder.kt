package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.app.ActionBar
import android.content.Context
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

/**
 * Created by younghokim on 16. 8. 24..
 */
abstract class ATriggerViewHolder<T : OTTrigger>(parent: ViewGroup, val listener: ITriggerControlListener, context: Context) :
        RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.trigger_list_element, parent, false))
        , View.OnClickListener {

    interface ITriggerControlListener {
        fun onTriggerRemove(position: Int)
        fun onTriggerEdited(position: Int)
    }

    protected lateinit var trigger: T
        private set

    var isExpanded: Boolean = true
        private set

    private val triggerSwitch: Switch

    private val removeButton: View
    private val expandToggleButton: AppCompatImageButton

    private val typeIconView: AppCompatImageView
    private val typeDescriptionView: TextView
    private val typeWappen: View

    private val configSummaryView: TextView
    private val expandedView: ViewGroup
    private val headerViewContainer: ViewGroup

    init {
        triggerSwitch = itemView.findViewById(R.id.ui_trigger_switch) as Switch
        triggerSwitch.setOnClickListener(this)

        removeButton = itemView.findViewById(R.id.ui_button_remove)
        removeButton.setOnClickListener(this)

        expandToggleButton = itemView.findViewById(R.id.ui_button_expand_toggle) as AppCompatImageButton
        expandToggleButton.setOnClickListener(this)

        typeIconView = itemView.findViewById(R.id.ui_type_icon) as AppCompatImageView
        typeDescriptionView = itemView.findViewById(R.id.ui_type_description) as TextView
        typeWappen = itemView.findViewById(R.id.ui_wappen_type)

        configSummaryView = itemView.findViewById(R.id.ui_config_summary) as TextView
        expandedView = itemView.findViewById(R.id.ui_expanded_view) as ViewGroup

        headerViewContainer = itemView.findViewById(R.id.ui_header_view_container) as ViewGroup

        setIsExpanded(false, false)
    }

    private fun setIsExpanded(isExpanded: Boolean, animate: Boolean) {
        if (this.isExpanded != isExpanded) {
            this.isExpanded = isExpanded
            if (animate) {
                TransitionManager.beginDelayedTransition(itemView as ViewGroup)
            }

            if (isExpanded) {
                removeButton.visibility = View.VISIBLE
                configSummaryView.visibility = View.INVISIBLE
                expandToggleButton.setImageResource(R.drawable.up_dark)
                if (expandedView.childCount == 0) {
                    val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT)
                    expandedView.addView(initExpandedViewContent(), lp)
                }
                updateExpandedViewContent(expandedView.getChildAt(0), trigger)
                expandedView.visibility = View.VISIBLE
            } else {
                removeButton.visibility = View.INVISIBLE
                configSummaryView.visibility = View.VISIBLE
                expandToggleButton.setImageResource(R.drawable.down_dark)
                expandedView.visibility = View.GONE
            }
        }
    }

    fun bind(trigger: OTTrigger) {
        this.trigger = trigger as T

        syncViewStateToTrigger()
    }

    protected fun syncViewStateToTrigger() {
        triggerSwitch.isChecked = trigger.isOn

        configSummaryView.text = getConfigSummary(trigger)

        typeIconView.setImageResource(trigger.configIconId)
        typeDescriptionView.setText(trigger.configTitleId)

        onSyncViewStateToTrigger(trigger)
    }

    protected abstract fun onSyncViewStateToTrigger(trigger: T);

    protected abstract fun initExpandedViewContent(): View

    protected abstract fun updateExpandedViewContent(expandedView: View, trigger: T): Unit

    protected abstract fun getConfigSummary(trigger: T): CharSequence

    override fun onClick(view: View?) {
        if (view === removeButton) {
            listener.onTriggerRemove(adapterPosition)
        } else if (view === expandToggleButton) {
            setIsExpanded(!isExpanded, true)
        } else if (view === triggerSwitch) {
            trigger?.isOn = triggerSwitch.isChecked
            if (isExpanded) {
                setIsExpanded(false, true)
            }
        }
    }

}