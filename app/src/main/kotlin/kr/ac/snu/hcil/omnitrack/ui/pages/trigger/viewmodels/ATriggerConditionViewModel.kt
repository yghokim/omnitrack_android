package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels

import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.ATriggerCondition

/**
 * Created by younghokim on 2017. 11. 12..
 */
abstract class ATriggerConditionViewModel(val trigger: OTTriggerDAO, val conditionType: Byte) {
    abstract fun refreshDaoToFront(snapshot: OTTriggerDAO)
    abstract fun onSwitchChanged(isOn: Boolean)
    abstract fun afterTriggerFired(triggerTime: Long)

    abstract fun onDispose()

    fun <T : ATriggerCondition> getCondition(): T? {
        return trigger.condition as T?
    }
}