package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.app.Application
import io.reactivex.subjects.BehaviorSubject
import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.RealmResults
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.ATriggerCondition
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.RealmViewModel
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.onNextIfDifferAndNotNull
import java.util.*
import javax.inject.Inject

/**
 * Created by younghokim on 2017-10-24.
 */
class TriggerDetailViewModel(app: Application) : RealmViewModel(app), OrderedRealmCollectionChangeListener<RealmResults<OTTrackerDAO>> {

    companion object {
        const val MODE_NEW: Byte = 0
        const val MODE_EDIT: Byte = 1
    }

    @Inject
    protected lateinit var syncManager: OTSyncManager

    var triggerId: String? = null
        private set

    private var originalTriggerDao: OTTriggerDAO? = null
    private var attachedTrackersResult: RealmResults<OTTrackerDAO>? = null

    var isOffline: Boolean = false
        private set

    val viewModelMode = BehaviorSubject.create<Byte>()
    val actionType = BehaviorSubject.create<Byte>()
    val conditionType = BehaviorSubject.create<Byte>()
    val conditionInstance = BehaviorSubject.create<ATriggerCondition>()

    val script = BehaviorSubject.create<Nullable<String>>()
    val useScript = BehaviorSubject.createDefault(false)

    var isInitialized: Boolean = false
        private set

    private var attachedTrackersRealmResults: RealmResults<OTTrackerDAO>? = null
    private val currentAttachedTrackerInfoList = java.util.ArrayList<OTTrackerDAO.SimpleTrackerInfo>()
    val attachedTrackers = BehaviorSubject.createDefault<List<OTTrackerDAO.SimpleTrackerInfo>>(currentAttachedTrackerInfoList)

    override fun onInject(app: OTApp) {
        app.applicationComponent.inject(this)
    }

    fun initEdit(triggerId: String, userId: String) {
        if (!isInitialized) {
            isOffline = false
            viewModelMode.onNext(MODE_EDIT)
            this.triggerId = triggerId
            val dao = dbManager.get().makeTriggersOfUserQuery(userId, realm).equalTo("objectId", triggerId).findFirst()
            if (dao != null) {
                this.originalTriggerDao = dao
                this.attachedTrackersRealmResults = dao.liveTrackersQuery.findAllAsync()
                this.attachedTrackersRealmResults?.addChangeListener(this)
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

    override fun onCleared() {
        super.onCleared()
        attachedTrackersRealmResults?.removeAllChangeListeners()
    }

    fun getSerializedDao(): String? {
        return originalTriggerDao?.let { getApplication<OTApp>().daoSerializationComponent.manager().get().serializeTrigger(it) }
    }

    fun validateConfiguration(): List<CharSequence>? {
        val msgs = ArrayList<CharSequence>()
        if (conditionInstance.value.isConfigurationValid(msgs)) {
            return null
        } else return msgs
    }

    override fun onChange(snapshot: RealmResults<OTTrackerDAO>, changeSet: OrderedCollectionChangeSet?) {
        if (changeSet == null) {
            currentAttachedTrackerInfoList.clear()
            currentAttachedTrackerInfoList.addAll(snapshot.map { it.getSimpleInfo() })
        } else {
            val removes = changeSet.deletions.map { i -> currentAttachedTrackerInfoList[i] }
            currentAttachedTrackerInfoList.removeAll(removes)

            //deal with additions
            val newDaos = changeSet.insertions.map { i -> snapshot[i] }
            currentAttachedTrackerInfoList.addAll(
                    newDaos.mapNotNull { it?.getSimpleInfo() }
            )

            //deal with update
            changeSet.changes.forEach { index ->
                snapshot[index]?.getSimpleInfo()?.let { currentAttachedTrackerInfoList[index] = it }
            }
        }

        attachedTrackers.onNext(currentAttachedTrackerInfoList)
    }

    private fun applyToFront(dao: OTTriggerDAO) {
        actionType.onNextIfDifferAndNotNull(dao.actionType)
        conditionType.onNextIfDifferAndNotNull(dao.conditionType)
        conditionInstance.onNextIfDifferAndNotNull(dao.condition?.clone() as ATriggerCondition)
        if (isOffline) {
            currentAttachedTrackerInfoList.clear()
            currentAttachedTrackerInfoList.addAll(dao.trackers.map { it.getSimpleInfo() })
            attachedTrackers.onNext(currentAttachedTrackerInfoList)
        }

        script.onNextIfDifferAndNotNull(Nullable(dao.additionalScript))
        useScript.onNextIfDifferAndNotNull(dao.checkScript)
    }

    val isAttachedTrackersDirty: Boolean
        get() {
            val triggerDao = originalTriggerDao
            if (triggerDao == null) return true
            else {
                return !Arrays.equals(
                        attachedTrackers.value?.map { it.objectId }?.toTypedArray() ?: emptyArray(),
                        triggerDao.trackers.map { it.objectId }.toTypedArray()
                )
            }
        }

    val isDirty: Boolean
        get() {
            return (conditionInstance.value != originalTriggerDao?.condition || isAttachedTrackersDirty || originalTriggerDao?.checkScript != useScript.value || originalTriggerDao?.additionalScript != script.value?.datum)
        }

    fun saveFrontToDao() {
        val dao = originalTriggerDao
        if (dao != null && isDirty) {
            fun apply(dao: OTTriggerDAO) {
                dao.serializedCondition = conditionInstance.value.getSerializedString()
                dao.additionalScript = script.value?.datum?.let { if (it.isBlank()) null else it }
                dao.checkScript = useScript.value
                dao.trackers.clear()
                val trackerIds = attachedTrackers.value.mapNotNull { it.objectId }.toTypedArray()
                if (trackerIds.isNotEmpty()) {
                    val trackers = realm.where(OTTrackerDAO::class.java).`in`("objectId", attachedTrackers.value.mapNotNull { it.objectId }.toTypedArray()).findAll()
                    if (dao.isManaged) {
                        dao.trackers.addAll(trackers)
                    } else {
                        dao.trackers.addAll(realm.copyFromRealm(trackers))
                    }
                }
                dao.synchronizedAt = null
            }

            if (dao.isManaged) {
                realm.executeTransaction {
                    apply(dao)
                    dbManager.get().saveTrigger(dao, realm)
                }
                syncManager.registerSyncQueue(ESyncDataType.TRIGGER, SyncDirection.UPLOAD)
            } else {
                apply(dao)
            }
        }
    }
}