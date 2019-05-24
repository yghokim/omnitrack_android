package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels

import android.content.Context
import io.reactivex.Completable
import io.reactivex.subjects.BehaviorSubject
import io.realm.*
import kr.ac.snu.hcil.android.common.onNextIfDifferAndNotNull
import kr.ac.snu.hcil.android.common.view.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerInformationHelper
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerSystemManager
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTTriggerAction
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.ATriggerCondition
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTDataDrivenTriggerCondition
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.OTTriggerViewFactory
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionAsObservable
import java.util.*
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 10. 24..
 */
open class TriggerViewModel(val context: Context, val dao: OTTriggerDAO, val realm: Realm) : IReadonlyObjectId, RealmChangeListener<OTTriggerDAO>, OrderedRealmCollectionChangeListener<RealmResults<OTTrackerDAO>> {

    @Inject
    protected lateinit var syncManager: OTSyncManager

    @Inject
    protected lateinit var triggerSystemManager: OTTriggerSystemManager

    override val _id: String?
        get() = dao._id

    val triggerActionType: BehaviorSubject<Byte> = BehaviorSubject.create()
    val triggerAction = BehaviorSubject.create<OTTriggerAction>()
    val triggerConditionType: BehaviorSubject<Byte> = BehaviorSubject.create()
    val triggerCondition: BehaviorSubject<ATriggerCondition> = BehaviorSubject.create()
    val triggerId: BehaviorSubject<String> = BehaviorSubject.create()

    val deletionLocked: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)
    val editionLocked: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)
    val switchLocked: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)

    val configIconResId: BehaviorSubject<Int> = BehaviorSubject.create()
    val configDescResId: BehaviorSubject<Int> = BehaviorSubject.create()

    val triggerSwitch: BehaviorSubject<Boolean> = BehaviorSubject.create()

    val configSummary: BehaviorSubject<CharSequence> = BehaviorSubject.create()

    val scriptUsed = BehaviorSubject.createDefault(false)

    private var attachedTrackersRealmResults: RealmResults<OTTrackerDAO>? = null
    private val currentAttachedTrackerInfoList = ArrayList<OTTrackerDAO.SimpleTrackerInfo>()
    val attachedTrackers = BehaviorSubject.createDefault<List<OTTrackerDAO.SimpleTrackerInfo>>(currentAttachedTrackerInfoList)


    private var currentConditionViewModel: ATriggerConditionViewModel? = null
        set(value) {
            if (field != value) {
                field?.onDispose()
                field = value
            }
        }

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)

        applyDaoToFront()
        println("trigger user id: ${dao.userId}")
        if (dao.isManaged) {
            dao.addChangeListener(this)
            attachedTrackersRealmResults = dao.liveTrackersQuery.findAllAsync()
            attachedTrackersRealmResults?.addChangeListener(this)
        }
    }

    fun getConditionViewModel(): ATriggerConditionViewModel? {
        return currentConditionViewModel
    }

    fun onFired(triggerTime: Long) {
        println("trigger fired at $triggerTime")
        currentConditionViewModel?.afterTriggerFired(triggerTime)
    }

    fun refreshCondition() {
        if (triggerCondition.value is OTDataDrivenTriggerCondition) {
            dao.invalidateConditionCache()
            val newCondition = dao.condition
            if (newCondition != null) {
                triggerCondition.onNext(newCondition)
            }
        }
    }

    fun applyDaoToFront() {
        dao.initialize(true)
        triggerActionType.onNext(dao.actionType)
        dao.action?.let { triggerAction.onNext(it) }
        if (currentConditionViewModel?.conditionType != dao.conditionType) {
            currentConditionViewModel = OTTriggerViewFactory.getConditionViewProvider(dao.conditionType)?.getTriggerConditionViewModel(dao, context)
        }
        currentConditionViewModel?.refreshDaoToFront(dao)

        triggerConditionType.onNext(dao.conditionType)
        dao.condition?.let { triggerCondition.onNext(it) }

        configIconResId.onNextIfDifferAndNotNull(OTTriggerInformationHelper.getConfigIconResId(dao))
        configDescResId.onNextIfDifferAndNotNull(OTTriggerInformationHelper.getConfigDescResId(dao))
        configSummary.onNextIfDifferAndNotNull(OTTriggerInformationHelper.getConfigSummaryText(dao, context))
        scriptUsed.onNextIfDifferAndNotNull(dao.checkScript && dao.additionalScript?.isNotBlank() == true)
        triggerSwitch.onNextIfDifferAndNotNull(dao.isOn)

        editionLocked.onNextIfDifferAndNotNull(dao.isEditingLocked())
        deletionLocked.onNextIfDifferAndNotNull(dao.isDeletionLocked())
        switchLocked.onNextIfDifferAndNotNull(dao.isSwitchLocked())

        if (!dao.isManaged) {

        }
    }

    override fun onChange(snapshot: OTTriggerDAO) {
        if (snapshot.isValid && snapshot.isLoaded) {
            applyDaoToFront()
        }
    }

    override fun onChange(snapshot: RealmResults<OTTrackerDAO>, changeSet: OrderedCollectionChangeSet) {
        if (changeSet.state == OrderedCollectionChangeSet.State.INITIAL) {
            currentAttachedTrackerInfoList.clear()
            currentAttachedTrackerInfoList.addAll(snapshot.map { it.getSimpleInfo() })
        } else { //deal with deletions
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

    fun apply(newDao: OTTriggerDAO) {
        if (!dao.isManaged || dao != newDao) {
            dao.conditionType = newDao.conditionType
            dao.serializedCondition = newDao.serializedCondition
            dao.actionType = newDao.actionType
            dao.serializedAction = newDao.serializedAction
            dao.trackers.clear()
            dao.trackers.addAll(newDao.trackers)

            dao.checkScript = newDao.checkScript
            dao.additionalScript = newDao.additionalScript

            applyDaoToFront()
        }
    }


    fun toggleSwitchAsync(): Completable {

        return Completable.defer {
            return@defer turnSwitchAsync(!(this.triggerSwitch.value ?: false))
        }
    }

    fun turnSwitchAsync(on: Boolean): Completable {
        return Completable.defer {
            if (on) {
                if (dao.isOn) {
                    Completable.complete()
                } else {
                    if (dao.isManaged) {
                        return@defer dao.isValidToTurnOn(context)
                                .flatMapCompletable { (validationError) ->
                                    if (validationError == null) {
                                        val id = dao._id
                                        realm.executeTransactionAsObservable { realm ->
                                            realm.where(OTTriggerDAO::class.java).equalTo("_id", id).findFirst()
                                                    ?.apply {
                                                        isOn = true
                                                        synchronizedAt = null
                                                    }
                                        }.doOnComplete {
                                            triggerSwitch.onNextIfDifferAndNotNull(true)
                                            triggerSystemManager.handleTriggerOn(dao)
                                        }.doAfterTerminate {
                                            syncManager.registerSyncQueue(ESyncDataType.TRIGGER, SyncDirection.UPLOAD, ignoreDirtyFlags = false)
                                        }
                                    } else {
                                        Completable.error(OTTriggerDAO.TriggerConfigInvalidException(*validationError))
                                    }
                                }
                    } else {
                        //offline mode
                        println("offline mode trigger switch on")
                        return@defer dao.isValidToTurnOn(context)
                                .flatMapCompletable { (validationError) ->
                                    if (validationError == null ||
                                            (validationError.size == 1 &&
                                                    validationError.contains(OTTriggerDAO.TriggerInvalidReason.TRACKER_NOT_ATTACHED))) {
                                        dao.isOn = true
                                        triggerSwitch.onNextIfDifferAndNotNull(true)
                                        Completable.complete()
                                    } else {
                                        Completable.error(OTTriggerDAO.TriggerConfigInvalidException(*validationError))
                                    }
                                }
                    }
                }
            } else {
                if (dao.isOn) {
                    if (dao.isManaged) {
                        val id = dao._id
                        realm.executeTransactionAsObservable { realm ->
                            realm.where(OTTriggerDAO::class.java).equalTo("_id", id).findFirst()
                                    ?.apply {
                                        isOn = false
                                        synchronizedAt = null
                                    }
                        }.doOnComplete {
                            triggerSwitch.onNextIfDifferAndNotNull(false)
                            triggerSystemManager.handleTriggerOff(dao)
                        }.doAfterTerminate {
                            syncManager.registerSyncQueue(ESyncDataType.TRIGGER, SyncDirection.UPLOAD, ignoreDirtyFlags = false)
                        }
                    } else {
                        println("offline mode trigger switch off")
                        dao.isOn = false
                        triggerSwitch.onNextIfDifferAndNotNull(false)
                        Completable.complete()
                    }
                } else Completable.complete()
            }
        }
    }

    fun unregister() {
        if (dao.isManaged) {
            dao.removeChangeListener(this)
            attachedTrackersRealmResults?.removeChangeListener(this)
        }
        currentConditionViewModel?.onDispose()
    }

}