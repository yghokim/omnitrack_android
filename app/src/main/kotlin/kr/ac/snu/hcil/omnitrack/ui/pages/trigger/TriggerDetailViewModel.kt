package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.app.Application
import android.content.Context
import android.os.Bundle
import com.google.gson.JsonObject
import dagger.Lazy
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.RealmResults
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.onNextIfDifferAndNotNull
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.DaoSerializationManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.flags.CreationFlagsHelper
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTTriggerAction
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.ATriggerCondition
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.RealmViewModel
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
    @Inject
    protected lateinit var serializationManager: Lazy<DaoSerializationManager>

    var triggerId: String? = null
        private set

    private var originalTriggerDao: OTTriggerDAO? = null

    var isOffline: Boolean = false
        private set

    val viewModelMode = BehaviorSubject.create<Byte>()
    val actionType = BehaviorSubject.create<Byte>()
    val conditionType = BehaviorSubject.create<Byte>()
    val conditionInstance = BehaviorSubject.create<ATriggerCondition>()
    val actionInstance = BehaviorSubject.create<OTTriggerAction>()

    val script = BehaviorSubject.create<Nullable<String>>()
    val useScript = BehaviorSubject.createDefault(false)

    val lockedProperties: JsonObject?
        get() {
            return originalTriggerDao?.getParsedLockedPropertyInfo()
        }

    var isInitialized: Boolean = false
        private set

    private var attachedTrackersRealmResults: RealmResults<OTTrackerDAO>? = null
    private val currentAttachedTrackerInfoList = java.util.ArrayList<OTTrackerDAO.SimpleTrackerInfo>()
    val attachedTrackers = BehaviorSubject.createDefault<List<OTTrackerDAO.SimpleTrackerInfo>>(currentAttachedTrackerInfoList)

    private var ignoreInitialTrackerList = false

    override fun onInject(app: OTAndroidApp) {
        app.applicationComponent.inject(this)
    }

    fun initEdit(triggerId: String, userId: String, savedInstanceState: Bundle?) {
        if (!isInitialized) {
            isOffline = false
            viewModelMode.onNext(MODE_EDIT)
            this.triggerId = triggerId
            val dao = dbManager.get().makeTriggersOfUserVisibleQuery(userId, realm).equalTo("_id", triggerId).findFirst()
            if (dao != null) {
                this.originalTriggerDao = dao
                this.attachedTrackersRealmResults = dao.liveTrackersQuery.findAllAsync()
                this.attachedTrackersRealmResults?.addChangeListener(this)
                if (savedInstanceState != null) {
                    ignoreInitialTrackerList = true
                    applySavedInstanceStateToFront(savedInstanceState)
                } else applyToFront(dao)
            }
            isInitialized = true
        }
    }

    fun initEdit(baseTriggerDao: OTTriggerDAO, savedInstanceState: Bundle?) {
        if (!isInitialized) {
            isOffline = true
            viewModelMode.onNext(MODE_EDIT)
            this.originalTriggerDao = baseTriggerDao
            applyToFront(baseTriggerDao)
            if (savedInstanceState != null) {
                ignoreInitialTrackerList = true
                applySavedInstanceStateToFront(savedInstanceState)
            }
            isInitialized = true
        }
    }

    fun initNew(baseTriggerDao: OTTriggerDAO, savedInstanceState: Bundle?) {
        if (!isInitialized) {
            isOffline = true
            viewModelMode.onNext(MODE_NEW)
            triggerId = null
            this.originalTriggerDao = baseTriggerDao
            applyToFront(baseTriggerDao)
            if (savedInstanceState != null) {
                ignoreInitialTrackerList = true
                applySavedInstanceStateToFront(savedInstanceState)
            }
            isInitialized = true
        }
    }

    private fun applySavedInstanceStateToFront(state: Bundle) {
        actionType.onNextIfDifferAndNotNull(state.getByte("actionType"))
        conditionType.onNextIfDifferAndNotNull(state.getByte("conditionType"))
        if (state.containsKey("actionInstance") || state.containsKey("conditionInstance")) {
            val tempDao = OTTriggerDAO()
            tempDao.serializedAction = state.getString("actionInstance")
            tempDao.serializedCondition = state.getString("conditionInstance")
            actionInstance.onNextIfDifferAndNotNull(tempDao.action)
            conditionInstance.onNextIfDifferAndNotNull(tempDao.condition)
        }
        script.onNextIfDifferAndNotNull(Nullable(state.getString("script")))
        useScript.onNextIfDifferAndNotNull(state.getBoolean("useScript"))

        val trackerIds = state.getStringArray("trackers")
        if (trackerIds?.isNotEmpty() == true) {
            attachedTrackers.onNext(realm.where(OTTrackerDAO::class.java)
                    .`in`(BackendDbManager.FIELD_OBJECT_ID, trackerIds).findAll().map { it.getSimpleInfo() })
        } else {
            attachedTrackers.onNext(emptyList())
        }
    }

    fun onSaveInstanceState(outState: Bundle) {
        actionType.value?.let { outState.putByte("actionType", it) }
        actionInstance.value?.let { outState.putString("actionInstance", it.getSerializedString()) }
        conditionType.value?.let { outState.putByte("conditionType", it) }
        conditionInstance.value?.let { outState.putString("conditionInstance", it.getSerializedString()) }
        script.value?.datum?.let { outState.putString("script", it) }
        useScript.value?.let { outState.putBoolean("useScript", it) }

        if (attachedTrackers.value?.isNotEmpty() == true) {
            outState.putStringArray("trackers", attachedTrackers.value!!.map { it._id }.toTypedArray())
        }
    }

    override fun onCleared() {
        super.onCleared()
        attachedTrackersRealmResults?.removeAllChangeListeners()
    }

    fun getSerializedDao(): String? {
        return originalTriggerDao?.let { serializationManager.get().serializeTrigger(it) }
    }

    fun validateConfiguration(context: Context): Single<Pair<Boolean, List<CharSequence>?>> {
        return conditionInstance.value?.isConfigurationValid(context)
                ?: Single.just<Pair<Boolean, List<CharSequence>?>>(Pair(false, listOf("No condition instance.")))
    }

    override fun onChange(snapshot: RealmResults<OTTrackerDAO>, changeSet: OrderedCollectionChangeSet) {
        if (changeSet.state == OrderedCollectionChangeSet.State.INITIAL) {
            currentAttachedTrackerInfoList.clear()
            currentAttachedTrackerInfoList.addAll(snapshot.map { it.getSimpleInfo() })

            if (ignoreInitialTrackerList) return // if the ViewModel was restored from the savedInstanceState, do not feed the list to front to avoid the race condition.
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
        actionInstance.onNextIfDifferAndNotNull(dao.action?.clone() as OTTriggerAction)
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
                        attachedTrackers.value?.map { it._id }?.toTypedArray() ?: emptyArray(),
                        triggerDao.trackers.map { it._id }.toTypedArray()
                )
            }
        }

    val isDirty: Boolean
        get() {
            return (conditionInstance.value != originalTriggerDao?.condition || actionInstance.value != originalTriggerDao?.action || isAttachedTrackersDirty || originalTriggerDao?.checkScript != useScript.value || originalTriggerDao?.additionalScript != script.value?.datum)
        }

    fun saveFrontToDao() {
        val dao = originalTriggerDao
        if (dao != null && isDirty) {
            fun apply(dao: OTTriggerDAO) {
                dao.serializedCondition = conditionInstance.value?.getSerializedString()
                dao.serializedAction = actionInstance.value?.getSerializedString()
                dao.additionalScript = script.value?.datum?.let { if (it.isBlank()) null else it }
                dao.checkScript = useScript.value ?: false
                dao.trackers.clear()
                attachedTrackers.value?.let {
                    val trackerIds = it.mapNotNull { it._id }.toTypedArray()
                    if (trackerIds.isNotEmpty()) {
                        val trackers = realm.where(OTTrackerDAO::class.java).`in`("_id", trackerIds).findAll()
                        if (dao.isManaged) {
                            dao.trackers.addAll(trackers)
                        } else {
                            dao.trackers.addAll(realm.copyFromRealm(trackers))
                        }

                        if (dao.actionType == OTTriggerDAO.ACTION_TYPE_REMIND &&
                                trackers.firstOrNull()?.experimentIdInFlags != null &&
                                trackers.firstOrNull()?.experimentIdInFlags != dao.experimentIdInFlags) {
                            //if the trigger is a reminder, set experiment flag following the tracker.
                            dao.experimentIdInFlags = trackers.first()?.experimentIdInFlags
                            dao.serializedCreationFlags = CreationFlagsHelper.Builder(trackers.first()?.experimentIdInFlags
                                    ?: "{}")
                                    .setExperiment(trackers.first()!!.experimentIdInFlags!!)
                                    .build()
                        }
                    }

                }


                if (BuildConfig.DEFAULT_EXPERIMENT_ID != null && dao.experimentIdInFlags == null) {
                    dao.experimentIdInFlags = BuildConfig.DEFAULT_EXPERIMENT_ID
                    dao.serializedCreationFlags = CreationFlagsHelper.Builder(dao.serializedCreationFlags)
                            .setInjected(false)
                            .setExperiment(BuildConfig.DEFAULT_EXPERIMENT_ID)
                            .build()
                }

                dao.synchronizedAt = null
            }

            if (dao.isManaged) {
                realm.executeTransaction {
                    apply(dao)
                    dbManager.get().saveTrigger(dao, realm)
                }
                syncManager.registerSyncQueue(ESyncDataType.TRIGGER, SyncDirection.UPLOAD, ignoreDirtyFlags = false)
            } else {
                apply(dao)
            }
        }
    }
}