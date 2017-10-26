package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.ATriggerCondition
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


    var triggerId: String? = null
        private set

    private var originalTriggerDao: OTTriggerDAO? = null

    var isOffline: Boolean = false
        private set

    val viewModelMode = BehaviorSubject.create<Byte>()
    val actionType = BehaviorSubject.create<Byte>()
    val conditionType = BehaviorSubject.create<Byte>()
    val conditionInstance = BehaviorSubject.create<ATriggerCondition>()

    var isInitialized: Boolean = false
        private set

    fun initEdit(triggerId: String, userId: String) {
        if (!isInitialized) {
            isOffline = false
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

    fun initEdit(baseTriggerDao: OTTriggerDAO) {
        if (!isInitialized) {
            isOffline = true
            viewModelMode.onNext(MODE_EDIT)
            this.originalTriggerDao = baseTriggerDao
            applyToFront(baseTriggerDao)
        }
        isInitialized = true
    }

    fun initNew(baseTriggerDao: OTTriggerDAO) {
        if (!isInitialized) {
            isOffline = true
            viewModelMode.onNext(MODE_NEW)
            triggerId = null
            this.originalTriggerDao = baseTriggerDao
            applyToFront(baseTriggerDao)
            isInitialized = true
        }
    }

    fun getSerializedDao(): String? {
        return originalTriggerDao?.let { OTTriggerDAO.parser.toJson(it, OTTriggerDAO::class.java) }
    }

    fun validateConfiguration(): List<CharSequence>? {
        val msgs = ArrayList<CharSequence>()
        if (conditionInstance.value.isConfigurationValid(msgs)) {
            return null
        } else return msgs
    }

    private fun applyToFront(dao: OTTriggerDAO) {
        actionType.onNextIfDifferAndNotNull(dao.actionType)
        conditionType.onNextIfDifferAndNotNull(dao.conditionType)
        conditionInstance.onNextIfDifferAndNotNull(dao.condition?.clone() as ATriggerCondition)
    }

    val isDirty: Boolean
        get() {
            return viewModelMode.value == MODE_EDIT && conditionInstance.value != originalTriggerDao?.condition
        }

    fun saveFrontToDao() {
        val dao = originalTriggerDao
        if (dao != null) {

            fun apply(dao: OTTriggerDAO) {
                dao.serializedCondition = conditionInstance.value.getSerializedString()
            }

            if (dao.isManaged) {
                realm.executeTransaction { apply(dao) }
            } else {
                apply(dao)
            }
        }
    }
}