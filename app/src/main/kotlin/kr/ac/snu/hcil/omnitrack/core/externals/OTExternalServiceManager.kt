package kr.ac.snu.hcil.omnitrack.core.externals

import android.content.Context
import android.content.SharedPreferences
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.core.externals.fitbit.FitbitService
import kr.ac.snu.hcil.omnitrack.core.externals.google.fit.GoogleFitService
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
                        GoogleFitService(context, preferences),
                        FitbitService(context, preferences),
                        MisfitService(context, preferences),
                        RescueTimeService(context, preferences)
                        //,MicrosoftBandService
                        //,MiBandService
                ).filter { service -> service.isSupportedInSystem(this) }.toTypedArray()

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

    fun registerApiKey(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    fun removeApiKey(key: String) {
        preferences.edit().remove(key).apply()
    }

    fun getApiKey(key: String): String? {
        return if (preferences.contains(key)) {
            preferences.getString(key, null)
        } else {
            BuildConfig.apiKeyTable.get(key)
        }
    }
}