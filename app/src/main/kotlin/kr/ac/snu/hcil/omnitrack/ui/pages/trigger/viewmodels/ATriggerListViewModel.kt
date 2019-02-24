package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels

import android.app.Application
import android.os.Bundle
import androidx.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.RealmViewModel
import java.util.*
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 10. 24..
 */

abstract class ATriggerListViewModel(app: Application) : RealmViewModel(app) {

    @Inject
    protected lateinit var authManager: OTAuthManager

    protected val currentTriggerViewModels = ArrayList<TriggerViewModel>()
    protected val _currentTriggerViewModelListObservable = BehaviorSubject.create<List<TriggerViewModel>>()

    val currentTriggerViewModelListObservable: Observable<List<TriggerViewModel>> get() = _currentTriggerViewModelListObservable
    @StringRes open val emptyMessageResId: Int = R.string.msg_trigger_empty

    open val defaultTriggerInterfaceOptions: TriggerInterfaceOptions = TriggerInterfaceOptions()

    override fun onInject(app: OTAndroidApp) {
        app.applicationComponent.inject(this)
    }

    protected open fun beforeAddNewTrigger(daoToAdd: OTTriggerDAO) {
        daoToAdd.userId = authManager.userId
    }

    fun addNewTrigger(unManagedDAO: OTTriggerDAO) {
        if (unManagedDAO.objectId == null) {
            unManagedDAO.objectId = UUID.randomUUID().toString()
        }
        addNewTriggerImpl(unManagedDAO)
    }

    protected abstract fun addNewTriggerImpl(dao: OTTriggerDAO)

    abstract fun removeTrigger(objectId: String)

    open fun onSaveInstanceState(outState: Bundle) {}

    protected fun notifyNewTriggerViewModels() {
        _currentTriggerViewModelListObservable.onNext(currentTriggerViewModels)
    }

}