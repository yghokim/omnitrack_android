package kr.ac.snu.hcil.omnitrack.ui.pages.services

import android.arch.lifecycle.ViewModel
import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.ui.components.common.dependency.DependencyControlViewModel
import rx.Observable
import rx.Single
import rx.subjects.BehaviorSubject
import rx.subscriptions.CompositeSubscription

/**
 * Created by younghokim on 2017. 5. 25..
 */
class ServiceActivationViewModel : ViewModel() {

    enum class State { Checking, IdleNotSatistified, Satisfied }

    private val subscriptions = CompositeSubscription()

    var attachedService: OTExternalService? = null
        set(value) {
            if (field != value) {
                field = value
                subscriptions.clear()
                if (value != null) {
                    serviceThumbnailResId.onNext(value.thumbResourceId)
                    serviceNameResId.onNext(value.nameResourceId)
                    serviceDescResId.onNext(value.descResourceId)
                    currentDependencyViewModels = value.dependencyList.map { DependencyControlViewModel(it) }
                } else {
                    serviceThumbnailResId.onNext(0)
                    serviceNameResId.onNext(0)
                    serviceDescResId.onNext(0)
                    currentDependencyViewModels = null
                }
            }
        }

    val currentState: BehaviorSubject<State> = BehaviorSubject.create()
    var serviceThumbnailResId: BehaviorSubject<Int> = BehaviorSubject.create()
    var serviceNameResId: BehaviorSubject<Int> = BehaviorSubject.create()
    var serviceDescResId: BehaviorSubject<Int> = BehaviorSubject.create()

    var serviceDependencyViewModels: BehaviorSubject<List<DependencyControlViewModel>> = BehaviorSubject.create()


    private var currentDependencyViewModels: List<DependencyControlViewModel>? = null
        set(value) {
            if (field != value) {
                field?.forEach {
                    it.dispose()
                }

                field = value

                value?.let {
                    subscriptions.add(
                            Observable.merge(it.map { model -> model.onStatusChanged.map { state -> Pair(model, state) } })
                                    .subscribe {
                                        pair ->
                                        currentState.onNext(combineCurrentDependencyViewModelState())
                                    }
                    )
                }

                serviceDependencyViewModels.onNext(value)
            }
        }

    init {
        currentState.onNext(State.IdleNotSatistified)
    }

    private fun combineCurrentDependencyViewModelState(): State {
        var nullCount = 0
        var fatalFailedExists = false
        currentDependencyViewModels?.forEach { model ->
            if (model.onStatusChanged.hasValue()) {
                if (model.onStatusChanged.value == DependencyControlViewModel.State.CHECKING || model.onStatusChanged.value == DependencyControlViewModel.State.RESOLVING) {
                    return State.Checking
                } else if (model.onStatusChanged.value == DependencyControlViewModel.State.FAILED_FATAL) {
                    fatalFailedExists = true
                }
            } else {
                nullCount++
            }
        }

        if (fatalFailedExists) {
            return State.IdleNotSatistified
        } else return if (nullCount == 0) State.Satisfied else State.IdleNotSatistified
    }

    fun startDependencyCheck(context: Context): Boolean {
        if (currentState.value == State.Checking) {
            return false
        }

        val dependencyModels = serviceDependencyViewModels.value ?: return false

        dependencyModels.forEach { it.checkDependency(context) }

        val observables = dependencyModels.map { it.onStatusChanged.takeUntil { state -> state != DependencyControlViewModel.State.CHECKING } }

        /*
        dependencyCheckSubscriptions.add(
                Observable.zip<State>(observables, {
                    stateArray ->
                    if (stateArray.filter { it == DependencyControlViewModel.State.FAILED_FATAL }.isNotEmpty()) {
                        State.IdleNotSatistified
                    } else {
                        State.Satisfied
                    }
                }
                ).doOnNext {
                    state ->
                    currentState.onNext(state)
                }
                        .subscribe {
                            state ->
                            println("Service Activation Dependency Check finished.")
                        }
        )*/


        return true
    }

    fun activateService(): Single<Boolean> {
        return attachedService?.activateSilently() ?: Single.just(false)
    }

    override fun onCleared() {
        super.onCleared()
        subscriptions.clear()
    }

}