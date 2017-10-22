package kr.ac.snu.hcil.omnitrack.core.database

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTTimeTriggerCondition

/**
 * Created by younghokim on 2017. 10. 21..
 */
object OTTriggerInformationHelper {

    fun getConfigSummaryText(trigger: OTTriggerDAO): CharSequence? {
        return null
    }

    @StringRes
    fun getConfigDescRestId(trigger: OTTriggerDAO): Int? {
        return when (trigger.conditionType) {
            OTTriggerDAO.CONDITION_TYPE_TIME -> {
                when ((trigger.condition as OTTimeTriggerCondition).timeConditionType) {
                    OTTimeTriggerCondition.TIME_CONDITION_ALARM -> R.string.msg_trigger_time_config_desc_alarm
                    OTTimeTriggerCondition.TIME_CONDITION_INTERVAL -> R.string.msg_trigger_time_config_desc_interval
                    else -> null
                }
            }
            OTTriggerDAO.CONDITION_TYPE_DATA -> R.string.trigger_desc_time
            else -> null
        }
    }

    @DrawableRes
    fun getConfigIconResId(trigger: OTTriggerDAO): Int? {
        return when (trigger.conditionType) {
            OTTriggerDAO.CONDITION_TYPE_TIME -> {
                when ((trigger.condition as OTTimeTriggerCondition).timeConditionType) {
                    OTTimeTriggerCondition.TIME_CONDITION_ALARM -> R.drawable.alarm_dark
                    OTTimeTriggerCondition.TIME_CONDITION_INTERVAL -> R.drawable.repeat_dark
                    else -> null
                }
            }
            OTTriggerDAO.CONDITION_TYPE_DATA -> R.drawable.event_dark
            else -> null
        }
    }

    @StringRes
    fun getActionName(actionType: Byte): Int? {
        return when (actionType) {
            OTTriggerDAO.ACTION_TYPE_LOG -> R.string.msg_text_trigger
            OTTriggerDAO.ACTION_TYPE_REMIND -> R.string.msg_text_reminder
            else -> null
        }
    }
}