package kr.ac.snu.hcil.omnitrack.core.triggers

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTTimeTriggerCondition
import kr.ac.snu.hcil.omnitrack.utils.BitwiseOperationHelper

/**
 * Created by younghokim on 2017. 10. 21..
 */
object OTTriggerInformationHelper {

    fun getConfigSummaryText(trigger: OTTriggerDAO): CharSequence? {
        return when (trigger.conditionType) {
            OTTriggerDAO.CONDITION_TYPE_TIME -> {
                val condition = trigger.condition as OTTimeTriggerCondition
                if (condition.isRepeated) {
                    //display only days of weeks
                    if (condition.dayOfWeekFlags == 0b1111111.toByte()) {
                        OTApp.instance.resourcesWrapped.getString(R.string.msg_everyday)
                    } else {

                        val names = OTApp.instance.resourcesWrapped.getStringArray(R.array.days_of_week_short)

                        val stringBuilder = StringBuilder()

                        for (day in 0..6) {

                            if (BitwiseOperationHelper.getBooleanAt(condition.dayOfWeekFlags.toInt(), 6 - day)) {
                                stringBuilder.append(names[day].toUpperCase(), "  ")
                            }
                        }

                        stringBuilder.trim()
                    }
                } else OTApp.instance.resourcesWrapped.getString(R.string.msg_once)
            }
            else -> null
        }
    }

    @StringRes
    fun getConfigDescResId(trigger: OTTriggerDAO): Int? {
        return when (trigger.conditionType) {
            OTTriggerDAO.CONDITION_TYPE_TIME -> getTimeConfigDescResId((trigger.condition as OTTimeTriggerCondition).timeConditionType)
            OTTriggerDAO.CONDITION_TYPE_DATA -> R.string.trigger_desc_time
            else -> null
        }
    }

    @DrawableRes
    fun getConfigIconResId(trigger: OTTriggerDAO): Int? {
        return when (trigger.conditionType) {
            OTTriggerDAO.CONDITION_TYPE_TIME -> getTimeConfigIconResId((trigger.condition as OTTimeTriggerCondition).timeConditionType)
            OTTriggerDAO.CONDITION_TYPE_DATA -> R.drawable.event_dark
            else -> null
        }
    }

    @StringRes
    fun getTimeConfigDescResId(timeConditionType: Byte): Int? {
        return when (timeConditionType) {
            OTTimeTriggerCondition.TIME_CONDITION_ALARM -> R.string.msg_trigger_time_config_desc_alarm
            OTTimeTriggerCondition.TIME_CONDITION_INTERVAL -> R.string.msg_trigger_time_config_desc_interval
            OTTimeTriggerCondition.TIME_CONDITION_SAMPLING -> R.string.msg_trigger_time_config_desc_ema
            else -> null
        }
    }

    @DrawableRes
    fun getTimeConfigIconResId(timeConditionType: Byte): Int? {
        return when (timeConditionType) {
                    OTTimeTriggerCondition.TIME_CONDITION_ALARM -> R.drawable.alarm_dark
                    OTTimeTriggerCondition.TIME_CONDITION_INTERVAL -> R.drawable.repeat_dark
            OTTimeTriggerCondition.TIME_CONDITION_SAMPLING -> R.drawable.ic_alarm_ema_24px
                    else -> null
                }
    }

    @StringRes
    fun getActionNameResId(actionType: Byte): Int? {
        return when (actionType) {
            OTTriggerDAO.ACTION_TYPE_LOG -> R.string.msg_text_trigger
            OTTriggerDAO.ACTION_TYPE_REMIND -> R.string.msg_text_reminder
            else -> null
        }
    }
}