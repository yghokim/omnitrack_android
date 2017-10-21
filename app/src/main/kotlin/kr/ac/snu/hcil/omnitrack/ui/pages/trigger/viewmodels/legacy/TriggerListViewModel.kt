package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.legacy

import android.support.v7.util.DiffUtil
import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTimeTrigger
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.UserAttachedViewModel

/**
 * Created by Young-Ho on 6/4/2017.
 */
class TriggerListViewModel(var triggerFilter: (OTTrigger) -> Boolean = { trigger -> true }) : UserAttachedViewModel() {

    private val currentTriggerViewModels = ArrayList<TriggerViewModel<OTTrigger>>()

    val triggerViewModelListSubject: BehaviorSubject<List<TriggerViewModel<OTTrigger>>> = BehaviorSubject.create()

    override fun onDispose() {
        super.onDispose()
        clearTriggerList()
    }

    private fun makeTriggerViewModel(trigger: OTTrigger): TriggerViewModel<OTTrigger> {
        return if (trigger is OTTimeTrigger) {
            TimeTriggerViewModel(trigger)
        } else TriggerViewModel(trigger)
    }

    override fun onUserAttached(userId: String) {
        super.onUserAttached(userId)
        clearTriggerList()
        /*
        currentTriggerViewModels.addAll(newUser.triggerManager.getFilteredTriggers(triggerFilter).map {
            makeTriggerViewModel(it).apply {
                this.register()
            }
        })

        triggerViewModelListSubject.onNext(currentTriggerViewModels)

        internalSubscriptions.add(
                newUser.triggerManager.triggerAdded.filter(triggerFilter).subscribe {
                    trigger ->
                    currentTriggerViewModels.add(
                            makeTriggerViewModel(trigger).apply {
                                this.register()
                            }
                    )
                    triggerViewModelListSubject.onNext(currentTriggerViewModels)
                }
        )

        internalSubscriptions.add(
                newUser.triggerManager.triggerRemoved.filter(triggerFilter).subscribe {
                    trigger ->
                    val toRemove = currentTriggerViewModels.find { it.trigger.objectId == trigger.objectId }
                    toRemove?.let {
                        currentTriggerViewModels.remove(toRemove)
                        triggerViewModelListSubject.onNext(currentTriggerViewModels)
                    }
                }
        )
        */
    }

    fun addTrigger(trigger: OTTrigger) {
        //user?.triggerManager?.putNewTrigger(trigger)
    }

    fun removeTrigger(trigger: OTTrigger) {
        //user?.triggerManager?.removeTrigger(trigger)
    }

    fun removeTrigger(viewModel: TriggerViewModel<OTTrigger>) {
        removeTrigger(viewModel.trigger)
    }

    private fun clearTriggerList() {
        currentTriggerViewModels.forEach {
            it.unregister()
        }

        currentTriggerViewModels.clear()

        triggerViewModelListSubject.onNext(emptyList())
    }

    class TriggerViewModelListDiffUtilCallback(val oldList: List<TriggerViewModel<OTTrigger>>, val newList: List<TriggerViewModel<OTTrigger>>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].trigger.objectId == newList[newItemPosition].trigger.objectId
        }

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return areItemsTheSame(oldItemPosition, newItemPosition)
        }


    }
}