package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels

import android.support.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.utils.RealmViewModel
import java.util.*

/**
 * Created by younghokim on 2017. 10. 24..
 */

abstract class ATriggerListViewModel : RealmViewModel() {

    protected val currentTriggerViewModels = ArrayList<TriggerViewModel>()
    protected val _currentTriggerViewModelListObservable = BehaviorSubject.create<List<TriggerViewModel>>()

    val currentTriggerViewModelListObservable: Observable<List<TriggerViewModel>> get() = _currentTriggerViewModelListObservable
    @StringRes open val emptyMessageResId: Int = R.string.msg_trigger_empty

    open val defaultTriggerInterfaceOptions: TriggerInterfaceOptions = TriggerInterfaceOptions()

    protected open fun beforeAddNewTrigger(daoToAdd: OTTriggerDAO) {
        daoToAdd.userId = OTAuthManager.userId
    }

    fun addNewTrigger(unManagedDAO: OTTriggerDAO) {
        if (unManagedDAO.objectId == null) {
            unManagedDAO.objectId = UUID.randomUUID().toString()
        }
        addNewTriggerImpl(unManagedDAO)
    }

    protected abstract fun addNewTriggerImpl(dao: OTTriggerDAO)

    abstract fun removeTrigger(objectId: String)

    protected fun notifyNewTriggerViewModels() {
        _currentTriggerViewModelListObservable.onNext(currentTriggerViewModels)
    }

}