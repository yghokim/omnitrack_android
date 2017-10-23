package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels

import android.arch.lifecycle.ViewModel
import android.support.annotation.StringRes
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.realm.*
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.OTTriggerInformationHelper
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.utils.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionAsObservable
import kr.ac.snu.hcil.omnitrack.utils.onNextIfDifferAndNotNull
import java.util.*

/**
 * Created by younghokim on 2017. 10. 20..
 *
 * This class is a ViewModel of Android Architecture Component to be used for displaying and editing the triggers of a specific user.
 *
 * @author Young-Ho Kim
 *
 *
 */
abstract class ATriggerListViewModel : ViewModel() {
    protected val realm = OTApp.instance.databaseManager.getRealmInstance()

    private val currentTriggerViewModels = ArrayList<TriggerViewModel>()
    private val _currentTriggerViewModelListObservable = BehaviorSubject.create<List<TriggerViewModel>>()

    val currentTriggerViewModelListObservable: Observable<List<TriggerViewModel>> get() = _currentTriggerViewModelListObservable
    private var currentTriggerRealmResults: RealmResults<OTTriggerDAO>? = null

    /**
     * @param originalQuery Receives the original query in vanilla viewmodel. modify this
     * @return the modified query for Triggers. The vanilla implementation just returns the originalQuery.
     */
    protected open fun hookTriggerQuery(originalQuery: RealmQuery<OTTriggerDAO>): RealmQuery<OTTriggerDAO> {
        return originalQuery
    }

    @StringRes open val emptyMessageResId: Int = R.string.msg_trigger_empty

    open val defaultTriggerInterfaceOptions: TriggerInterfaceOptions = TriggerInterfaceOptions()


    protected fun init() {
        onDispose()

        currentTriggerRealmResults = hookTriggerQuery(realm.where(OTTriggerDAO::class.java).equalTo(RealmDatabaseManager.FIELD_REMOVED_BOOLEAN, false))
                .findAllAsync()

        currentTriggerRealmResults?.addChangeListener { snapshot, changeSet ->
            if (changeSet == null) {
                //initial set
                currentTriggerViewModels.clear()
                currentTriggerViewModels.addAll(
                        snapshot.map { TriggerViewModel(it) }
                )
            } else {
                //changes
                //deal with deletions
                val removes = changeSet.deletions.map { i -> currentTriggerViewModels[i] }
                removes.forEach { it.unregister() }
                currentTriggerViewModels.removeAll(removes)

                //deal with additions
                changeSet.insertions.forEach { i ->
                    currentTriggerViewModels.add(i, TriggerViewModel(snapshot[i]!!))
                }

                //deal with refresh
                changeSet.changes.forEach { i ->
                }
            }

            _currentTriggerViewModelListObservable.onNext(currentTriggerViewModels)
        }
    }

    protected open fun beforeAddNewTrigger(daoToAdd: OTTriggerDAO) {
        daoToAdd.userId = OTAuthManager.userId
    }

    fun addNewTrigger(unManagedDAO: OTTriggerDAO) {
        if (unManagedDAO.objectId == null) {
            unManagedDAO.objectId = UUID.randomUUID().toString()
        }

        beforeAddNewTrigger(unManagedDAO)

        OTApp.instance.databaseManager.addNewTrigger(unManagedDAO, realm)
    }

    fun removeTrigger(objectId: String) {
        val viewModel = currentTriggerViewModels.find { it.objectId == objectId }
        if (viewModel != null) {
            OTApp.instance.databaseManager.removeTrigger(viewModel.dao, realm)
        }
    }

    inner open class TriggerViewModel(val dao: OTTriggerDAO) : IReadonlyObjectId, RealmChangeListener<OTTriggerDAO>, OrderedRealmCollectionChangeListener<RealmResults<OTTrackerDAO>> {

        override val objectId: String?
            get() = dao.objectId

        val triggerActionType: BehaviorSubject<Byte> = BehaviorSubject.create()
        val triggerConditionType: BehaviorSubject<Byte> = BehaviorSubject.create()
        val triggerId: BehaviorSubject<String> = BehaviorSubject.create()

        val configIconResId: BehaviorSubject<Int> = BehaviorSubject.create()
        val configDescResId: BehaviorSubject<Int> = BehaviorSubject.create()

        val triggerSwitch: BehaviorSubject<Boolean> = BehaviorSubject.create()

        val configSummary: BehaviorSubject<CharSequence> = BehaviorSubject.create()

        private var attachedTrackersRealmResults: RealmResults<OTTrackerDAO>? = null
        private val currentAttachedTrackerInfoList = ArrayList<Pair<Int, String>>()
        val attachedTrackers = BehaviorSubject.createDefault<List<Pair<Int, String>>>(currentAttachedTrackerInfoList)

        init {
            applyDaoToFront()
            dao.addChangeListener(this)
            attachedTrackersRealmResults = dao.liveTrackersQuery.findAllAsync()
            attachedTrackersRealmResults?.addChangeListener(this)
        }

        private fun applyDaoToFront() {
            triggerActionType.onNext(dao.actionType)
            triggerConditionType.onNext(dao.conditionType)

            configIconResId.onNextIfDifferAndNotNull(OTTriggerInformationHelper.getConfigIconResId(dao))
            configDescResId.onNextIfDifferAndNotNull(OTTriggerInformationHelper.getConfigDescRestId(dao))
            configSummary.onNextIfDifferAndNotNull(OTTriggerInformationHelper.getConfigSummaryText(dao))

            triggerSwitch.onNextIfDifferAndNotNull(dao.isOn)
        }

        override fun onChange(snapshot: OTTriggerDAO) {
            if (snapshot.isValid && snapshot.isLoaded) {
                applyDaoToFront()
            }
        }

        override fun onChange(snapshot: RealmResults<OTTrackerDAO>, changeSet: OrderedCollectionChangeSet?) {
            if (changeSet == null) {
                currentAttachedTrackerInfoList.clear()
                currentAttachedTrackerInfoList.addAll(snapshot.map { Pair(it.color, it.name) })
            } else { //deal with deletions
                val removes = changeSet.deletions.map { i -> currentAttachedTrackerInfoList[i] }
                currentAttachedTrackerInfoList.removeAll(removes)

                //deal with additions
                val newDaos = changeSet.insertions.map { i -> snapshot[i] }
                currentAttachedTrackerInfoList.addAll(
                        newDaos.mapNotNull { it?.let { Pair(it.color, it.name) } }
                )

                //deal with update
                changeSet.changes.forEach { index ->
                    snapshot[index]?.let { Pair(it.color, it.name) }?.let { currentAttachedTrackerInfoList[index] = it }
                }
            }

            attachedTrackers.onNext(currentAttachedTrackerInfoList)
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
                        //TODO handle trigger on
                        val validationError = dao.isValidToTurnOn()
                        if (validationError == null) {
                            val id = dao.objectId
                            realm.executeTransactionAsObservable { realm ->
                                realm.where(OTTriggerDAO::class.java).equalTo("objectId", id).findFirst()
                                        ?.isOn = true
                            }.doOnComplete { triggerSwitch.onNextIfDifferAndNotNull(true) }
                        } else Completable.error(validationError)
                    }
                } else {
                    //TODO handle trigger off in system
                    if (dao.isOn == true) {
                        val id = dao.objectId
                        realm.executeTransactionAsObservable { realm ->
                            realm.where(OTTriggerDAO::class.java).equalTo("objectId", id).findFirst()
                                    ?.isOn = false
                        }.doOnComplete { triggerSwitch.onNextIfDifferAndNotNull(false) }
                    } else Completable.complete()
                }
            }
        }

        fun unregister() {
            dao.removeChangeListener(this)
            attachedTrackersRealmResults?.removeChangeListener(this)
        }


    }

    internal fun onDispose() {
        currentTriggerViewModels.clear()
        currentTriggerRealmResults?.removeAllChangeListeners()
    }

    override fun onCleared() {
        super.onCleared()
        realm.close()
    }
}