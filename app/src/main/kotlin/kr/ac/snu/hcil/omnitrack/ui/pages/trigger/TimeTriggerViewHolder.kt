package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.view.View
import android.view.ViewGroup
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTimeTrigger

/**
 * Created by younghokim on 16. 8. 24..
 */
class TimeTriggerViewHolder : ATriggerViewHolder<OTTimeTrigger> {

    constructor(parent: ViewGroup, listener: ITriggerControlListener, context: Context) : super(parent, listener, context)

    override fun getConfigSummary(trigger: OTTimeTrigger): CharSequence {
        if (trigger.isConfigSpecified) {

        }

        if (trigger.isRangeSpecified && trigger.isRepeated) {
            //display only days of weeks
            if (OTTimeTrigger.Range.isAllDayUsed(trigger.rangeVariables)) {
                return itemView.context.resources.getString(R.string.msg_everyday)
            } else {

                val names = itemView.context.resources.getStringArray(R.array.days_of_week_short)

                val stringBuilder = StringBuilder()

                for (day in 0..6) {
                    if (OTTimeTrigger.Range.isDayOfWeekUsed(trigger.rangeVariables, day)) {
                        stringBuilder.append(names[day].toUpperCase(), "  ")
                    }
                }

                return stringBuilder.trim()
            }
        } else return itemView.context.resources.getString(R.string.msg_once)
    }


    override fun getHeaderView(current: View?, trigger: OTTimeTrigger): View {
        val view = if (current is TimeTriggerDisplayView) current else TimeTriggerDisplayView(itemView.context)

        view.nextTriggerTime = trigger.getNextAlarmTime(trigger.lastTriggeredTime)

        when (trigger.configType) {
            OTTimeTrigger.CONFIG_TYPE_ALARM ->
                view.setAlarmInformation(OTTimeTrigger.AlarmConfig.getHour(trigger.configVariables),
                        OTTimeTrigger.AlarmConfig.getMinute(trigger.configVariables),
                        OTTimeTrigger.AlarmConfig.getAmPm(trigger.configVariables))

            OTTimeTrigger.CONFIG_TYPE_INTERVAL ->
                view.setIntervalInformation(OTTimeTrigger.IntervalConfig.getIntervalSeconds(trigger.configVariables))
        }

        return view
    }

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
    }


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
    }


}