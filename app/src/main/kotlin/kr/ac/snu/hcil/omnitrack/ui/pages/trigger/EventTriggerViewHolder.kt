package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.core.triggers.OTEventTrigger

/**
 * Created by younghokim on 16. 9. 5..
 */
class EventTriggerViewHolder : ATriggerViewHolder<OTEventTrigger> {

    constructor(parent: ViewGroup, listener: ITriggerControlListener, context: Context) : super(parent, listener, context)

    override fun getConfigSummary(trigger: OTEventTrigger): CharSequence {
        if (trigger.measure == null || trigger.conditioner == null) {
            return "Not enough information"
        } else {
            return "Listening to event"
        }
    }

    override fun getHeaderView(current: View?, trigger: OTEventTrigger): View {
        return TextView(itemView.context)
    }

    override fun initExpandedViewContent(): View {
        return EventTriggerConfigurationPanel(itemView.context)
    }

    override fun updateExpandedViewContent(expandedView: View, trigger: OTEventTrigger) {
        if (expandedView is EventTriggerConfigurationPanel) {
            expandedView.conditioner = trigger.conditioner
            expandedView.selectedMeasureFactory = trigger.measure?.factory
        }
    }

    override fun updateTriggerWithViewSettings(expandedView: View, trigger: OTEventTrigger) {
        if (expandedView is EventTriggerConfigurationPanel) {
            println(expandedView.conditioner)
            println(expandedView.selectedMeasureFactory?.typeCode)
            trigger.conditioner = expandedView.conditioner
            trigger.measure = expandedView.selectedMeasureFactory?.makeMeasure()
        }
    }

    override fun validateExpandedViewInputs(expandedView: View, errorMessagesOut: MutableList<String>): Boolean {
        return true
    }

}