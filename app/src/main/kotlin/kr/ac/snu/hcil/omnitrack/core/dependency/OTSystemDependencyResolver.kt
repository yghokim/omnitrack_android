package kr.ac.snu.hcil.omnitrack.core.dependency

import android.app.Activity
import android.content.Context
import android.support.v4.app.Fragment
import rx.Single

/**
 * Created by younghokim on 2017. 5. 17..
 * This class indicates weather specific component is available in current Android system
 */
abstract class OTSystemDependencyResolver {
    enum class DependencyState {FatalFailed, NonFatalFailed, Passed }

    data class DependencyCheckResult(val state: DependencyState, val message: String, val resolveText: String)

    abstract fun checkDependencySatisfied(context: Context, selfResolve: Boolean): Single<DependencyCheckResult>

    abstract fun tryResolve(activity: Activity): Single<Boolean>
    open fun tryResolve(fragment: Fragment): Single<Boolean> {
        return tryResolve(fragment.activity)
    }
}