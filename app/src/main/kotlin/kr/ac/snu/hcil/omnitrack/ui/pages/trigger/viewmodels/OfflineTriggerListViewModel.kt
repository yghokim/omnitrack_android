package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels

import android.app.Application
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTriggerDAO

/**
 * Created by younghokim on 2017. 10. 24..
 */
open class OfflineTriggerListViewModel(app: Application) : ATriggerListViewModel(app) {

    fun init() {
        currentTriggerViewModels.forEach { it.unregister() }
        currentTriggerViewModels.clear()
    }

    override fun addNewTriggerImpl(dao: OTTriggerDAO) {
        if (dao.objectId != null) {
            val match = currentTriggerViewModels.find { it.objectId == dao.objectId }
            if (match != null) {
                match.apply(dao)
                return
            }
        }

        currentTriggerViewModels.add(TriggerViewModel(getApplication<OTApp>(), dao, realm))
        notifyNewTriggerViewModels()
    }

    override fun removeTrigger(objectId: String) {
        currentTriggerViewModels.find { it.objectId == objectId }?.let {
            it.unregister()
            currentTriggerViewModels.remove(it)
            notifyNewTriggerViewModels()
        }
    }
}