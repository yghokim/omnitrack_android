package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels

import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO

/**
 * Created by younghokim on 2017. 11. 12..
 */
abstract class ATriggerConditionViewModel(val trigger: OTTriggerDAO, val conditionType: Byte) {
    abstract fun refreshDaoToFront(snapshot: OTTriggerDAO)
    abstract fun onSwitchChanged(isOn: Boolean)
    abstract fun afterTriggerFired(triggerTime: Long)

    abstract fun onDispose()
}