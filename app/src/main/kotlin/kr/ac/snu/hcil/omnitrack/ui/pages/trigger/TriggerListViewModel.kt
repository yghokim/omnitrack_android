package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.UserAttachedViewModel
import rx.subjects.BehaviorSubject

/**
 * Created by Young-Ho on 6/4/2017.
 */
class TriggerListViewModel(var triggerFilter: (OTTrigger) -> Boolean = { trigger -> true }) : UserAttachedViewModel() {

    private val currentTriggerViewModels = ArrayList<TriggerViewModel>()

    private val triggerViewModelList: BehaviorSubject<List<TriggerViewModel>> = BehaviorSubject.create()

    override fun onDispose() {
        super.onDispose()
        clearTriggerList()
    }

    override fun onUserAttached(newUser: OTUser) {
        super.onUserAttached(newUser)
        clearTriggerList()
    }

    private fun clearTriggerList() {
        currentTriggerViewModels.forEach {
            it.unregister()
        }

        currentTriggerViewModels.clear()

        triggerViewModelList.onNext(emptyList())
    }
}