package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels

import android.content.Context
import io.reactivex.disposables.CompositeDisposable
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO

class DataDrivenConditionViewModel(trigger: OTTriggerDAO, context: Context) : ATriggerConditionViewModel(trigger, OTTriggerDAO.CONDITION_TYPE_DATA) {

    private val subscriptions = CompositeDisposable()


    override fun refreshDaoToFront(snapshot: OTTriggerDAO) {

    }

    override fun onSwitchChanged(isOn: Boolean) {

    }

    override fun afterTriggerFired(triggerTime: Long) {

    }

    override fun onDispose() {
        subscriptions.clear()
    }

}