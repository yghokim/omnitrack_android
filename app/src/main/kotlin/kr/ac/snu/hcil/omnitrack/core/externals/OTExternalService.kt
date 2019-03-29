package kr.ac.snu.hcil.omnitrack.core.externals

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.fragment.app.Fragment
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.android.common.INameDescriptionResourceProvider
import kr.ac.snu.hcil.omnitrack.core.dependency.OTSystemDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.dependency.PermissionDependencyResolver
import kr.ac.snu.hcil.omnitrack.ui.pages.services.ExternalServiceActivationActivity
import rx_activity_result2.RxActivityResult
import java.util.*

/**
 * Created by younghokim on 16. 7. 28..
 */
abstract class OTExternalService(val context: Context, val preferences: SharedPreferences, val identifier: String, val minimumSDK: Int) : INameDescriptionResourceProvider {


    enum class ServiceState {
        DEACTIVATED, ACTIVATING, ACTIVATED
    }

    open val isInternetRequiredForActivation = true

    private val _dependencyList = ArrayList<OTSystemDependencyResolver>()
    val dependencyList: List<OTSystemDependencyResolver> get() = _dependencyList

    open val permissionGranted: Boolean
        get() {
            for (permission in getRequiredPermissionsRecursive()) {
                if (context.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            return true
        }

    private val _measureFactories = ArrayList<OTServiceMeasureFactory>()

    val measureFactories: List<OTServiceMeasureFactory>
        get() {
        return _measureFactories
    }

    var state: ServiceState
        get() = onStateChanged.value ?: ServiceState.DEACTIVATED
        protected set(value) {
            if (onStateChanged.value != value) {
                onStateChanged.onNext(value)
            }
        }

    val onStateChanged: BehaviorSubject<ServiceState> = BehaviorSubject.create<ServiceState>().apply { onNext(ServiceState.DEACTIVATED) }

    init {
        state = if (getIsActivatedFlag(identifier) == true) {
            ServiceState.ACTIVATED
        } else {
            ServiceState.DEACTIVATED
        }
    }

    fun getIsActivatedFlag(serviceIdentifier: String): Boolean {
        return preferences.getBoolean(serviceIdentifier + "_activated", false)
    }

    fun setIsActivatedFlag(service: OTExternalService, isActivated: Boolean) {
        preferences.edit().putBoolean(service.identifier + "_activated", isActivated).apply()
    }

    internal fun initialize() {
        _measureFactories += onRegisterMeasureFactories()

        val permissions = getRequiredPermissionsRecursive()
        if (permissions.isNotEmpty()) {
            _dependencyList.add(
                    PermissionDependencyResolver(*permissions)
            )
        }

        _dependencyList += onRegisterDependencies()
    }

    internal abstract fun isSupportedInSystem(serviceManager: OTExternalServiceManager): Boolean

    protected abstract fun onRegisterMeasureFactories(): Array<OTServiceMeasureFactory>

    protected open fun onRegisterDependencies(): Array<OTSystemDependencyResolver> {
        return emptyArray()
    }

    fun activateSilently(): Single<Boolean> {
        return prepareServiceAsync()
                .map {
                    resultState ->
                    resultState >= OTSystemDependencyResolver.DependencyState.NonFatalFailed
                }
                .doOnSuccess {
                    result ->
                    if (result == true) {
                        setIsActivatedFlag(this, true)
                        state = ServiceState.ACTIVATED
                    } else {
                        setIsActivatedFlag(this, false)
                        state = ServiceState.DEACTIVATED
                    }
                }
    }

    fun startActivationActivityAsync(context: Context): Observable<Boolean> {

        return RxActivityResult.on(context as Activity)
                .startIntent(ExternalServiceActivationActivity.makeIntent(context, this))
                .map {
                    result ->
                    result.resultCode() == Activity.RESULT_OK
                }
    }


    fun deactivate(): Completable {
        return Completable.defer {
            setIsActivatedFlag(this, false)
            state = ServiceState.DEACTIVATED
            onDeactivate()
        }

    }

    abstract fun onDeactivate(): Completable

    abstract val thumbResourceId: Int

    fun grantPermissions(caller: Fragment, requestCode: Int) {
        caller.requestPermissions(getRequiredPermissionsRecursive(), requestCode)
    }

    open fun prepareServiceAsync(): Single<OTSystemDependencyResolver.DependencyState> {

        return Single.zip(dependencyList.map { it.checkDependencySatisfied(context, false) },
                { results ->
                    OTSystemDependencyResolver.combineDependencyState(*(results.map {
                        (it as OTSystemDependencyResolver.DependencyCheckResult).state
                    }.toTypedArray()))
                })

    }

    protected fun getRequiredPermissionsRecursive(): Array<String> {
        val result = HashSet<String>()

        for (factory in _measureFactories) {
            result.addAll(factory.requiredPermissions)
        }

        return result.toTypedArray()
    }
}