package kr.ac.snu.hcil.omnitrack.core.externals

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.support.v4.app.Fragment
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.externals.fitbit.FitbitService
import kr.ac.snu.hcil.omnitrack.core.externals.google.fit.GoogleFitService
import kr.ac.snu.hcil.omnitrack.core.externals.misfit.MisfitService
import kr.ac.snu.hcil.omnitrack.core.externals.rescuetime.RescueTimeService
import kr.ac.snu.hcil.omnitrack.utils.FillingIntegerIdReservationTable
import kr.ac.snu.hcil.omnitrack.utils.INameDescriptionResourceProvider
import kr.ac.snu.hcil.omnitrack.utils.events.Event
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


        var pendingConnectionListener: ((Boolean) -> Unit)? = null

        init {
            for (service in availableServices) {
                for (factory in service.measureFactories) {
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

    open val permissionGranted: Boolean
        get() {
            for (permission in requiredPermissionsRecursive) {
                if (OTApplication.app.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            return true
        }

    protected open val _measureFactories = ArrayList<OTMeasureFactory>()

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
    }

    fun activateAsync(context: Context, connectedHandler: ((Boolean) -> Unit)? = null) {
        state = ServiceState.ACTIVATING

        pendingConnectionListener = {
            result ->
            if (result == true) {
                setIsActivatedFlag(this, true)
                state = ServiceState.ACTIVATED
            } else {
                setIsActivatedFlag(this, false)
                state = ServiceState.DEACTIVATED
            }

            connectedHandler?.invoke(result)
            pendingConnectionListener = null
        }

        onActivateAsync(context, pendingConnectionListener)
    }

    abstract fun onActivateAsync(context: Context, connectedHandler: ((Boolean) -> Unit)? = null)


    fun deactivate() {
        setIsActivatedFlag(this, false)
        state = ServiceState.DEACTIVATED
        onDeactivate()
    }

    abstract fun onDeactivate()

    val activated = Event<Any>()
    val deactivated = Event<Any>()

    open val requiredPermissions: Array<String> = arrayOf()

    abstract val thumbResourceId: Int

    fun grantPermissions(caller: Fragment, requestCode: Int) {
        caller.requestPermissions(requiredPermissionsRecursive, requestCode)
    }

    abstract fun prepareServiceAsync(preparedHandler: ((Boolean) -> Unit)?)

    protected val requiredPermissionsRecursive: Array<String> by lazy {
        val result = HashSet<String>()

        result.addAll(requiredPermissions)
        for (factory in _measureFactories) {
            result.addAll(factory.requiredPermissions)
        }

        result.toTypedArray()
    }

    protected fun cancelActivationProcess() {
        pendingConnectionListener?.invoke(false)
        pendingConnectionListener = null
    }

    fun onActivityActivationResult(resultCode: Int, resultData: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            println("activation result failed")
            cancelActivationProcess()
        } else handleActivityActivationResultOk(resultData)
    }

    abstract fun handleActivityActivationResultOk(resultData: Intent?)
}