package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.view.View
import android.view.ViewGroup
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.legacy.TimeTriggerViewModel

/**
 * Created by younghokim on 16. 8. 24..
 *
 */
class TimeTriggerViewHolder(parent: ViewGroup, listener: ITriggerControlListener, context: Context) : ATriggerViewHolder<TimeTriggerViewModel>(parent, listener, context) {


    override fun getHeaderView(current: View?, viewModel: TimeTriggerViewModel): View {
        val view = if (current is TimeTriggerDisplayView) current else TimeTriggerDisplayView(itemView.context)

        if (current != view) {
            headerViewSubscriptions.clear()

            headerViewSubscriptions.add(
                    viewModel.nextAlarmTime.subscribe { nextAlarmTime ->
                        view.nextTriggerTime = nextAlarmTime ?: 0L
                    }
            )

            headerViewSubscriptions.add(
                    viewModel.configuredAlarmTime.subscribe {
                        time ->
                        println("configured alarm time changed: ${time}")
                        view.setAlarmInformation(time.hour, time.minute, time.amPm)
                    }
            )

            headerViewSubscriptions.add(
                    viewModel.configuredIntervalSeconds.subscribe {
                        seconds ->
                        println("configured interval changed: ${seconds}")
                        view.setIntervalInformation(seconds)
                    }
            )
        }
        return view
    }
/*
    override fun initExpandedViewContent(): View {
        return TimeTriggerConfigurationPanel(context = itemView.context)
    }

    override fun updateExpandedViewContent(expandedView: View, trigger: OTTimeTrigger) {
        if (expandedView is TimeTriggerConfigurationPanel) {
            expandedView.configMode = trigger.configType
            expandedView.IsRepeated = trigger.isRepeated
            expandedView.applyConfigVariables(trigger.configVariables)
            expandedView.applyRangeVariables(trigger.rangeVariables)
        }
    }*/

/*
    override fun updateTriggerWithViewSettings(expandedView: View, trigger: OTTimeTrigger) {
        if (expandedView is TimeTriggerConfigurationPanel) {
            trigger.configType = expandedView.configMode
            trigger.configVariables = expandedView.extractConfigVariables()
            trigger.rangeVariables = expandedView.extractRangeVariables()
            trigger.isRepeated = expandedView.IsRepeated
        }
    }


    override fun validateExpandedViewInputs(expandedView: View, errorMessagesOut: MutableList<String>): Boolean {
        if (expandedView is TimeTriggerConfigurationPanel) {
            return expandedView.validateExpandedViewInputs(errorMessagesOut)
        } else return true
    }*/


}