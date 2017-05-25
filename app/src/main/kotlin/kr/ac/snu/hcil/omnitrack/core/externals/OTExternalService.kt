package kr.ac.snu.hcil.omnitrack.core.externals

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.support.v4.app.Fragment
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.dependency.OTSystemDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.dependency.PermissionDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.externals.fitbit.FitbitService
import kr.ac.snu.hcil.omnitrack.core.externals.google.fit.GoogleFitService
import kr.ac.snu.hcil.omnitrack.core.externals.jawbone.JawboneUpService
import kr.ac.snu.hcil.omnitrack.core.externals.misfit.MisfitService
import kr.ac.snu.hcil.omnitrack.core.externals.rescuetime.RescueTimeService
import kr.ac.snu.hcil.omnitrack.utils.FillingIntegerIdReservationTable
import kr.ac.snu.hcil.omnitrack.utils.INameDescriptionResourceProvider
import rx.Observable
import java.util.*

/**
 * Created by younghokim on 16. 7. 28..
 */
abstract class OTExternalService(val identifier: String, val minimumSDK: Int) : INameDescriptionResourceProvider {
    companion object {

        val requestCodeDict = FillingIntegerIdReservationTable<OTExternalService>()

        fun assignRequestCode(service: OTExternalService) {
            requestCodeDict[service]
        }

        private val factoryCodeDict = HashMap<String, OTMeasureFactory>()

        val availableServices: Array<OTExternalService> by lazy {
            arrayOf(
                    //AndroidDeviceService,
                    GoogleFitService
                    ,FitbitService
                    , JawboneUpService
                    ,MisfitService
                    ,RescueTimeService
                    //,MicrosoftBandService
                    //,MiBandService
            )
        }

        fun getFilteredMeasureFactories(filter: (OTMeasureFactory) -> Boolean): List<OTMeasureFactory> {

            val list = ArrayList<OTMeasureFactory>()
            for (service in OTExternalService.availableServices) {
                for (factory in service.measureFactories) {
                    if (filter(factory)) {
                        list.add(factory)
                    }
                }
            }

            return list
        }

        fun getMeasureFactoryByCode(typeCode: String): OTMeasureFactory? {
            return factoryCodeDict[typeCode]
        }

        val preferences: SharedPreferences by lazy { OTApplication.app.getSharedPreferences("ExternalServices", Context.MODE_PRIVATE) }

        fun init() {
            for (service in availableServices) {
                for (factory in service.measureFactories) {
                    println("service: ${service.identifier}")
                    println("factory service: ${factory.getService()}")
                    factoryCodeDict.put(factory.typeCode, factory)
                }
            }
        }


        /***
         * Get whether the service's activation state stored in system
         */
        fun getIsActivatedFlag(service: OTExternalService): Boolean {
            return getIsActivatedFlag(service.identifier)
        }

        fun getIsActivatedFlag(serviceIdentifier: String): Boolean {
            return preferences.getBoolean(serviceIdentifier + "_activated", false)
        }

        fun setIsActivatedFlag(service: OTExternalService, isActivated: Boolean) {
            preferences.edit().putBoolean(service.identifier + "_activated", isActivated).apply()
        }
    }

    enum class ServiceState {
        DEACTIVATED, ACTIVATING, ACTIVATED
    }

    open val isInternetRequiredForActivation = true

    private val _dependencyList = ArrayList<OTSystemDependencyResolver>()
    val dependencyList: List<OTSystemDependencyResolver> get() = _dependencyList

    open val permissionGranted: Boolean
        get() {
            for (permission in getRequiredPermissionsRecursive()) {
                if (OTApplication.app.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            return true
        }

    private val _measureFactories = ArrayList<OTMeasureFactory>()

    val measureFactories: List<OTMeasureFactory> get() {
        return _measureFactories
    }

    var state: ServiceState = ServiceState.DEACTIVATED
        protected set


    init {

        state = if (getIsActivatedFlag(identifier) == true) {
            ServiceState.ACTIVATED
        } else {
            ServiceState.DEACTIVATED
        }

        initialize()
    }

    private fun initialize() {
        _measureFactories += onRegisterMeasureFactories()

        println("measure factories")
        println(_measureFactories)
        val permissions = getRequiredPermissionsRecursive()
        if (permissions.isNotEmpty()) {
            _dependencyList.add(
                    PermissionDependencyResolver(*permissions)
            )
        }

        _dependencyList += onRegisterDependencies()
    }

    protected abstract fun onRegisterMeasureFactories(): Array<OTMeasureFactory>

    protected open fun onRegisterDependencies(): Array<OTSystemDependencyResolver> {
        return emptyArray()
    }

    fun activateAsync(context: Context): Observable<Boolean> {

        return onActivateAsync(context)
                .doOnSubscribe {
                    state = ServiceState.ACTIVATING
                }
                .doOnNext {
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

    abstract fun onActivateAsync(context: Context): Observable<Boolean>


    fun deactivate() {
        setIsActivatedFlag(this, false)
        state = ServiceState.DEACTIVATED
        onDeactivate()
    }

    abstract fun onDeactivate()

    abstract val thumbResourceId: Int

    fun grantPermissions(caller: Fragment, requestCode: Int) {
        caller.requestPermissions(getRequiredPermissionsRecursive(), requestCode)
    }

    abstract fun prepareServiceAsync(preparedHandler: ((Boolean) -> Unit)?)

    protected fun getRequiredPermissionsRecursive(): Array<String> {
        val result = HashSet<String>()

        for (factory in _measureFactories) {
            result.addAll(factory.requiredPermissions)
        }

        return result.toTypedArray()
    }

    /*

    protected fun finishActivationProcess(success: Boolean) {
        pendingConnectionListener?.invoke(success)
        pendingConnectionListener = null
    }

    protected fun cancelActivationProcess() {
        finishActivationProcess(false)
    }

    fun onActivityActivationResult(resultCode: Int, resultData: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            println("activation result failed")
            cancelActivationProcess()
        } else handleActivityActivationResultOk(resultData)
    }*/

    //abstract fun handleActivityActivationResultOk(resultData: Intent?)
}