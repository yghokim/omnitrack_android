package kr.ac.snu.hcil.omnitrack.core.externals

import android.content.Context
import android.content.SharedPreferences
import kr.ac.snu.hcil.omnitrack.core.externals.fitbit.FitbitService
import kr.ac.snu.hcil.omnitrack.core.externals.google.fit.GoogleFitService
import kr.ac.snu.hcil.omnitrack.core.externals.jawbone.JawboneUpService
import kr.ac.snu.hcil.omnitrack.core.externals.misfit.MisfitService
import kr.ac.snu.hcil.omnitrack.core.externals.rescuetime.RescueTimeService
import java.util.*

class OTExternalServiceManager constructor(
        val context: Context,
        val preferences: SharedPreferences
) {

    init {
        println("externalServiceManager was created: ${this}")
    }

    private val factoryCodeDict = HashMap<String, OTServiceMeasureFactory>()

    val availableServices: Array<OTExternalService> by lazy {
        val list =
                arrayOf(
                        //AndroidDeviceService,
                        GoogleFitService(context),
                        FitbitService(context),
                        JawboneUpService(context),
                        MisfitService(context),
                        RescueTimeService(context)
                        //,MicrosoftBandService
                        //,MiBandService
                ).filter { service -> service.isSupportedInSystem() }.toTypedArray()

        for (service in list) {
            service.initialize()
            for (factory in service.measureFactories) {
                factoryCodeDict.put(factory.typeCode, factory)
            }
        }
        list
    }

    fun findServiceByIdentifier(identifier: String): OTExternalService? {
        return availableServices.find { it.identifier == identifier }
    }

    fun getFilteredMeasureFactories(filter: (OTServiceMeasureFactory) -> Boolean): List<OTServiceMeasureFactory> {

        val list = ArrayList<OTServiceMeasureFactory>()
        for (service in availableServices) {
            for (factory in service.measureFactories) {
                if (filter(factory)) {
                    list.add(factory)
                }
            }
        }

        return list
    }

    fun getMeasureFactoryByCode(typeCode: String): OTServiceMeasureFactory? {
        if (availableServices.isNotEmpty())
            return factoryCodeDict[typeCode]
        else return null
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