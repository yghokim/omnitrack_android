package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels

import kr.ac.snu.hcil.omnitrack.core.triggers.OTDataTrigger

/**
 * Created by younghokim on 2017. 6. 6..
 */
class DataTriggerViewModel(trigger: OTDataTrigger) : TriggerViewModel<OTDataTrigger>(trigger) {

    override fun register() {
        super.register()


        //TODO update config summary
        /*
        if (trigger.measure == null || trigger.conditioner == null) {
            return "Not enough information"
        } else {
            return "Listening to event"
        }*/
    }
}