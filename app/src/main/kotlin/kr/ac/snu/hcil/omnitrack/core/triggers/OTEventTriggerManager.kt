package kr.ac.snu.hcil.omnitrack.core.triggers

import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTEventTriggerCondition

class OTEventTriggerManager() {

    fun registerTrigger(trigger: OTTriggerDAO): Boolean {
        val condition = trigger.condition as OTEventTriggerCondition
        return condition.attachedEvent?.subscribe() ?: false
    }

    fun unregisterTrigger(trigger: OTTriggerDAO): Boolean {
        val condition = trigger.condition as OTEventTriggerCondition
        return condition.attachedEvent?.unsubscribe() ?: false
    }
}