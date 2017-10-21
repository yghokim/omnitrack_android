package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels

import android.arch.lifecycle.ViewModel
import android.support.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.realm.RealmChangeListener
import io.realm.RealmQuery
import io.realm.RealmResults
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.OTTriggerInformationHelper
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.utils.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.utils.onNextIfDifferAndNotNull

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

    private val realm = OTApp.instance.databaseManager.getRealmInstance()

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
            }

            _currentTriggerViewModelListObservable.onNext(currentTriggerViewModels)
        }

        onDispose()
    }

    fun remove(triggerId: String) {
        currentTriggerViewModels.find { it.objectId == triggerId }
                ?.dao?.let { triggerDao ->
            realm.executeTransactionAsync {
                triggerDao.removed = true
            }
        }
    }

    open class TriggerViewModel(val dao: OTTriggerDAO) : IReadonlyObjectId, RealmChangeListener<OTTriggerDAO> {

        override val objectId: String?
            get() = dao.objectId

        val triggerActionType: BehaviorSubject<Byte> = BehaviorSubject.create()
        val triggerConditionType: BehaviorSubject<Byte> = BehaviorSubject.create()
        val triggerId: BehaviorSubject<String> = BehaviorSubject.create()

        val configIconResId: BehaviorSubject<Int> = BehaviorSubject.create()
        val configDescResId: BehaviorSubject<Int> = BehaviorSubject.create()
        val configNameResId: BehaviorSubject<Int> = BehaviorSubject.create()

        val triggerSwitch: BehaviorSubject<Boolean> = BehaviorSubject.create()

        val configSummary: BehaviorSubject<CharSequence> = BehaviorSubject.create()

        init {
            applyDaoToFront()
            dao.addChangeListener(this)
        }

        private fun applyDaoToFront() {
            triggerActionType.onNext(dao.actionType)
            triggerConditionType.onNext(dao.conditionType)

            configIconResId.onNextIfDifferAndNotNull(OTTriggerInformationHelper.getConfigIconResId(dao))
            configDescResId.onNextIfDifferAndNotNull(OTTriggerInformationHelper.getConfigDescriptionResId(dao))
            configNameResId.onNextIfDifferAndNotNull(OTTriggerInformationHelper.getConfigNameRestId(dao))
            configSummary.onNextIfDifferAndNotNull(OTTriggerInformationHelper.getConfigSummaryText(dao))

            triggerSwitch.onNextIfDifferAndNotNull(dao.isOn)
        }

        override fun onChange(snapshot: OTTriggerDAO) {
            if (snapshot.isValid && snapshot.isLoaded) {
                applyDaoToFront()
            }
        }


        fun unregister() {
            dao.removeChangeListener(this)
        }
    }

    fun onDispose() {
        currentTriggerViewModels.clear()
        currentTriggerRealmResults?.removeAllChangeListeners()
    }

    override fun onCleared() {
        super.onCleared()
        realm.close()
    }
}