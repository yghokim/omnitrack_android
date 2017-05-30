package kr.ac.snu.hcil.omnitrack.ui.components.common.dependency

import android.app.Activity
import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.dependency.OTSystemDependencyResolver
import rx.subjects.BehaviorSubject
import rx.subscriptions.CompositeSubscription

/**
 * Created by younghokim on 2017. 5. 24..
 */
class DependencyControlViewModel(val dependencyResolver: OTSystemDependencyResolver) {

    enum class State {
        FAILED_FATAL, FAILED_NON_FATAL, SATISFIED, CHECKING, RESOLVING
    }

    val onStatusChanged: BehaviorSubject<State> = BehaviorSubject.create()
    val onDependencyCheckResult: BehaviorSubject<OTSystemDependencyResolver.DependencyCheckResult> = BehaviorSubject.create()

    private val subscriptions = CompositeSubscription()


    init {

    }

    fun checkDependency(context: Context) {
        if (onStatusChanged.value == State.CHECKING) {
            return
        }

        onStatusChanged.onNext(State.CHECKING)
        subscriptions.add(
                dependencyResolver.checkDependencySatisfied(context, true)
                        .doOnSuccess {
                            result ->
                            onStatusChanged.onNext(
                                    when (result.state) {
                                        OTSystemDependencyResolver.DependencyState.Passed -> State.SATISFIED
                                        OTSystemDependencyResolver.DependencyState.FatalFailed -> State.FAILED_FATAL
                                        OTSystemDependencyResolver.DependencyState.NonFatalFailed -> State.FAILED_NON_FATAL
                                        else -> State.FAILED_FATAL
                                    }
                            )

                            onDependencyCheckResult.onNext(result)
                        }
                        .doOnError {
                            onStatusChanged.onNext(State.FAILED_FATAL)
                        }
                        .subscribe({
                            result ->

                        }, {})
        )
    }

    fun resolveDependency(activity: Activity): Boolean {
        if (onStatusChanged.value != State.SATISFIED) {
            onStatusChanged.onNext(State.RESOLVING)
            subscriptions.add(dependencyResolver.tryResolve(activity)
                    .flatMap { result ->
                        dependencyResolver.checkDependencySatisfied(activity, true)
                    }.doOnSuccess {
                result ->
                onStatusChanged.onNext(
                        when (result.state) {
                            OTSystemDependencyResolver.DependencyState.Passed -> State.SATISFIED
                            OTSystemDependencyResolver.DependencyState.FatalFailed -> State.FAILED_FATAL
                            OTSystemDependencyResolver.DependencyState.NonFatalFailed -> State.FAILED_NON_FATAL
                            else -> State.FAILED_FATAL
                        }
                )
                onDependencyCheckResult.onNext(result)
            }.doOnError {
                onStatusChanged.onNext(State.FAILED_FATAL)
            }.subscribe({ }, { })
            )
            return true
        } else {
            return false
        }
    }

    fun dispose() {
        subscriptions.clear()
        onStatusChanged.onNext(State.FAILED_FATAL)
    }
}