package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels

import io.reactivex.Completable
import io.reactivex.subjects.BehaviorSubject
import io.realm.*
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.OTTriggerInformationHelper
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTTriggerAction
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.ATriggerCondition
import kr.ac.snu.hcil.omnitrack.utils.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionAsObservable
import kr.ac.snu.hcil.omnitrack.utils.onNextIfDifferAndNotNull
import java.util.*
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 10. 24..
 */
open class TriggerViewModel(app: OTApp, val dao: OTTriggerDAO, val realm: Realm) : IReadonlyObjectId, RealmChangeListener<OTTriggerDAO>, OrderedRealmCollectionChangeListener<RealmResults<OTTrackerDAO>> {

    @Inject
    protected lateinit var syncManager: OTSyncManager

    override val objectId: String?
        get() = dao.objectId

    val triggerActionType: BehaviorSubject<Byte> = BehaviorSubject.create()
    val triggerAction = BehaviorSubject.create<OTTriggerAction>()
    val triggerConditionType: BehaviorSubject<Byte> = BehaviorSubject.create()
    val triggerCondition: BehaviorSubject<ATriggerCondition> = BehaviorSubject.create()
    val triggerId: BehaviorSubject<String> = BehaviorSubject.create()

    val configIconResId: BehaviorSubject<Int> = BehaviorSubject.create()
    val configDescResId: BehaviorSubject<Int> = BehaviorSubject.create()

    val triggerSwitch: BehaviorSubject<Boolean> = BehaviorSubject.create()

    val configSummary: BehaviorSubject<CharSequence> = BehaviorSubject.create()

    private var attachedTrackersRealmResults: RealmResults<OTTrackerDAO>? = null
    private val currentAttachedTrackerInfoList = ArrayList<OTTrackerDAO.SimpleTrackerInfo>()
    val attachedTrackers = BehaviorSubject.createDefault<List<OTTrackerDAO.SimpleTrackerInfo>>(currentAttachedTrackerInfoList)


    init {
        app.synchronizationComponent.inject(this)

        applyDaoToFront()
        println("trigger user id: ${dao.userId}")
        if (dao.isManaged) {
            dao.addChangeListener(this)
            attachedTrackersRealmResults = dao.liveTrackersQuery.findAllAsync()
            attachedTrackersRealmResults?.addChangeListener(this)
        }
    }

    private fun applyDaoToFront() {
        dao.initialize(true)
        triggerActionType.onNext(dao.actionType)
        dao.action?.let { triggerAction.onNext(it) }
        triggerConditionType.onNext(dao.conditionType)
        dao.condition?.let { triggerCondition.onNext(it) }

        configIconResId.onNextIfDifferAndNotNull(OTTriggerInformationHelper.getConfigIconResId(dao))
        configDescResId.onNextIfDifferAndNotNull(OTTriggerInformationHelper.getConfigDescRestId(dao))
        configSummary.onNextIfDifferAndNotNull(OTTriggerInformationHelper.getConfigSummaryText(dao))

        triggerSwitch.onNextIfDifferAndNotNull(dao.isOn)

        if (!dao.isManaged) {

        }
    }

    override fun onChange(snapshot: OTTriggerDAO) {
        if (snapshot.isValid && snapshot.isLoaded) {
            applyDaoToFront()
        }
    }

    override fun onChange(snapshot: RealmResults<OTTrackerDAO>, changeSet: OrderedCollectionChangeSet?) {
        if (changeSet == null) {
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
            applyDaoToFront()
        }
    }


    fun toggleSwitchAsync(): Completable {

        return Completable.defer {
            return@defer turnSwitchAsync(!this.triggerSwitch.value)
        }
    }

    fun turnSwitchAsync(on: Boolean): Completable {
        return Completable.defer {
            if (on) {
                if (dao.isOn) {
                    Completable.complete()
                } else {
                    if (dao.isManaged) {
                        //TODO handle trigger on
                        val validationError = dao.isValidToTurnOn()
                        if (validationError == null) {
                            val id = dao.objectId
                            realm.executeTransactionAsObservable { realm ->
                                realm.where(OTTriggerDAO::class.java).equalTo("objectId", id).findFirst()
                                        ?.isOn = true
                            }.doOnComplete {
                                triggerSwitch.onNextIfDifferAndNotNull(true)
                            }.doAfterTerminate {
                                syncManager.registerSyncQueue(ESyncDataType.TRIGGER, SyncDirection.UPLOAD)
                            }
                        } else Completable.error(validationError)
                    } else {
                        //offline mode
                        println("offline mode trigger switch on")
                        val validationError = dao.isValidToTurnOn()
                        if (validationError == null ||
                                (validationError.causes.size == 1 &&
                                        validationError.causes.contains(OTTriggerDAO.TriggerValidationComponent.TRACKER_ATTACHED))) {
                            dao.isOn = true
                            triggerSwitch.onNextIfDifferAndNotNull(true)
                            Completable.complete()
                        } else {
                            Completable.error(validationError)
                        }
                    }
                }
            } else {
                //TODO handle trigger off in system
                if (dao.isOn == true) {
                    if (dao.isManaged) {
                        val id = dao.objectId
                        realm.executeTransactionAsObservable { realm ->
                            realm.where(OTTriggerDAO::class.java).equalTo("objectId", id).findFirst()
                                    ?.isOn = false
                        }.doOnComplete {
                            triggerSwitch.onNextIfDifferAndNotNull(false)
                        }.doAfterTerminate {
                            syncManager.registerSyncQueue(ESyncDataType.TRIGGER, SyncDirection.UPLOAD)
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
        dao.removeChangeListener(this)
        attachedTrackersRealmResults?.removeChangeListener(this)
    }

}