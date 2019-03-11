package kr.ac.snu.hcil.omnitrack.core.dependency

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import io.reactivex.Single

/**
 * Created by younghokim on 2017. 5. 17..
 * This class indicates weather specific component is available in current Android system
 */
abstract class OTSystemDependencyResolver {
    enum class DependencyState {FatalFailed, NonFatalFailed, Passed }

    data class DependencyCheckResult(val state: DependencyState, val message: CharSequence, val resolveText: CharSequence)

    companion object {
        fun combineDependencyState(vararg states: DependencyState): DependencyState {
            if (states.isNotEmpty()) {
                var currentState = DependencyState.Passed
                for (state in states) {
                    if (state == DependencyState.NonFatalFailed) {
                        currentState = DependencyState.NonFatalFailed
                    } else if (state == DependencyState.FatalFailed) {
                        return DependencyState.FatalFailed
                    }
                }
                return currentState
            } else {
                return DependencyState.Passed
            }
        }

    }

    abstract fun checkDependencySatisfied(context: Context, selfResolve: Boolean): Single<DependencyCheckResult>

    abstract fun tryResolve(activity: FragmentActivity): Single<Boolean>
    open fun tryResolve(fragment: Fragment): Single<Boolean> {
        return tryResolve(fragment.requireActivity())
    }
}