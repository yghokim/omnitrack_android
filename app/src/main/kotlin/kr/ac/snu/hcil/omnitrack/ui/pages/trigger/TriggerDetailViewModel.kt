package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.utils.RealmViewModel
import kr.ac.snu.hcil.omnitrack.utils.onNextIfDifferAndNotNull

/**
 * Created by younghokim on 2017-10-24.
 */
class TriggerDetailViewModel : RealmViewModel() {
    companion object {
        const val MODE_NEW: Byte = 0
        const val MODE_EDIT: Byte = 1
    }

    private var triggerId: String? = null
    private var originalTriggerDao: OTTriggerDAO? = null

    val viewModelMode = BehaviorSubject.create<Byte>()
    val actionType = BehaviorSubject.create<Byte>()
    val conditionType = BehaviorSubject.create<Byte>()

    var isInitialized: Boolean = false
        private set

    fun initEdit(triggerId: String, userId: String) {
        if (!isInitialized) {
            viewModelMode.onNext(MODE_EDIT)
            this.triggerId = triggerId
            val dao = OTApp.instance.databaseManager.makeTriggersOfUserQuery(userId, realm).equalTo("objectId", triggerId).findFirst()
            if (dao != null) {
                this.originalTriggerDao = dao
                applyToFront(dao)
            }
            isInitialized = true
        }
    }

    fun initNew(baseTriggerDao: OTTriggerDAO) {
        if (!isInitialized) {
            viewModelMode.onNext(MODE_NEW)
            triggerId = null
            applyToFront(baseTriggerDao)
            isInitialized = true
        }
    }

    private fun applyToFront(dao: OTTriggerDAO) {
        actionType.onNextIfDifferAndNotNull(dao.actionType)
        conditionType.onNextIfDifferAndNotNull(dao.conditionType)
    }
}