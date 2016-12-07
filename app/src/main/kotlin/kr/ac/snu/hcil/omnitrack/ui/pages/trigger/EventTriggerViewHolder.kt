package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.view.View
import android.view.ViewGroup
import kr.ac.snu.hcil.omnitrack.core.calculation.SingleNumericComparison
import kr.ac.snu.hcil.omnitrack.core.triggers.OTDataTrigger

/**
 * Created by younghokim on 16. 9. 5..
 */
class EventTriggerViewHolder(parent: ViewGroup, listener: ITriggerControlListener, context: Context) : ATriggerViewHolder<OTDataTrigger>(parent, listener, context) {

    override fun validateTriggerSwitchOn(): Boolean {
        println("validation: ${trigger.measure} , ${trigger.conditioner}")
        return super.validateTriggerSwitchOn() && trigger.measure != null && trigger.conditioner != null
    }

    override fun getConfigSummary(trigger: OTDataTrigger): CharSequence {
        if (trigger.measure == null || trigger.conditioner == null) {
            return "Not enough information"
        } else {
            return "Listening to event"
        }
    }

    override fun getHeaderView(current: View?, trigger: OTDataTrigger): View {

        val view = current as? EventTriggerDisplayView ?: EventTriggerDisplayView(itemView.context)
        view.setConditioner(trigger.conditioner as? SingleNumericComparison)
        view.setMeasureFactory(trigger.measure?.factory)

        return view
    }

    override fun initExpandedViewContent(): View {
        return EventTriggerConfigurationPanel(itemView.context)
    }

    override fun updateExpandedViewContent(expandedView: View, trigger: OTDataTrigger) {
        if (expandedView is EventTriggerConfigurationPanel) {
            expandedView.conditioner = trigger.conditioner
            expandedView.selectedMeasureFactory = trigger.measure?.factory
        }
    }

    override fun updateTriggerWithViewSettings(expandedView: View, trigger: OTDataTrigger) {
        if (expandedView is EventTriggerConfigurationPanel) {
            trigger.conditioner = expandedView.conditioner
            trigger.measure = expandedView.selectedMeasureFactory?.makeMeasure()
        }
    }

    override fun validateExpandedViewInputs(expandedView: View, errorMessagesOut: MutableList<String>): Boolean {
        return true
    }

    override fun getViewsForSwitchValidationFailedAlert(): Array<View>? {
        val superValue = super.getViewsForSwitchValidationFailedAlert()

        val childValue = if (trigger.measure == null || trigger.conditioner == null) {
            arrayOf(headerViewContainer as View)
        } else null

        return if (superValue != null && childValue != null) superValue + childValue
        else if (superValue != null && childValue == null) {
            superValue
        } else if (childValue != null && superValue == null) {
            childValue
        } else null
    }
}