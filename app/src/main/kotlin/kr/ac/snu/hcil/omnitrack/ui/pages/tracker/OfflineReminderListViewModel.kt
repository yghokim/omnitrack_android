package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.OfflineTriggerListViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.TriggerInterfaceOptions

/**
 * Created by younghokim on 2017. 10. 24..
 */
class OfflineReminderListViewModel : OfflineTriggerListViewModel() {
    override val defaultTriggerInterfaceOptions: TriggerInterfaceOptions = TriggerInterfaceOptions(false, null, arrayOf(OTTriggerDAO.CONDITION_TYPE_TIME), OTTriggerDAO.ACTION_TYPE_REMIND)
}