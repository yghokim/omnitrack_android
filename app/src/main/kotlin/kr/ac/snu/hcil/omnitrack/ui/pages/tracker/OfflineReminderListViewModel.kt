package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.OfflineTriggerListViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.TriggerInterfaceOptions

/**
 * Created by younghokim on 2017. 10. 24..
 */
class OfflineReminderListViewModel : OfflineTriggerListViewModel() {
    override val defaultTriggerInterfaceOptions: TriggerInterfaceOptions = TriggerInterfaceOptions(false, null, arrayOf(OTTriggerDAO.CONDITION_TYPE_TIME), OTTriggerDAO.ACTION_TYPE_REMIND)


    fun applyToDb(trackerId: String) {

        realm.executeTransactionAsync { realm ->
            currentTriggerViewModels.map { it.dao }.forEach {
                beforeAddNewTrigger(it)
                if (it.trackers.find { it.objectId == trackerId } == null) {
                    val trackerDao = realm.where(OTTrackerDAO::class.java).equalTo("objectId", trackerId).findFirst()
                    if (trackerDao != null)
                        it.trackers.add(realm.copyFromRealm(trackerDao))
                }
                OTApp.instance.databaseManager.saveTrigger(it, realm)
            }
        }
    }
}