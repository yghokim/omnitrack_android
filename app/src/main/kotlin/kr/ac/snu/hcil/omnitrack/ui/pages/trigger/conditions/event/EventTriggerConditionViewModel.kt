package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.event

import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.ATriggerConditionViewModel

class EventTriggerConditionViewModel(trigger: OTTriggerDAO, context: Context) : ATriggerConditionViewModel(trigger, OTTriggerDAO.CONDITION_TYPE_EVENT) {

    override fun refreshDaoToFront(snapshot: OTTriggerDAO) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSwitchChanged(isOn: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun afterTriggerFired(triggerTime: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDispose() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}