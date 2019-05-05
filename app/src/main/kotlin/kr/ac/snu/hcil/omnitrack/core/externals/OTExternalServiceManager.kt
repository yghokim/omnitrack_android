package kr.ac.snu.hcil.omnitrack.core.externals

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.core.externals.fitbit.FitbitService
import kr.ac.snu.hcil.omnitrack.core.externals.google.fit.GoogleFitService
import kr.ac.snu.hcil.omnitrack.core.externals.misfit.MisfitService
import kr.ac.snu.hcil.omnitrack.core.externals.rescuetime.RescueTimeService
import java.util.*

@Suppress("SimplifyBooleanWithConstants")
class OTExternalServiceManager constructor(
        val context: Context,
        val preferences: SharedPreferences
) {

    companion object {
        const val BROADCAST_ACTION_SERVICE_API_KEYS_CHANGED = "${BuildConfig.APPLICATION_ID}.action.service_api_keys_changed"

        val apiKeyChangedIntentFilter: IntentFilter by lazy {
            IntentFilter(BROADCAST_ACTION_SERVICE_API_KEYS_CHANGED)
        }
    }

    init {
        println("externalServiceManager was created: ${this}")
    }

    private val factoryCodeDict = HashMap<String, OTServiceMeasureFactory>()

    val installedServices: Array<OTExternalService> by lazy {
        arrayOf(
                //AndroidDeviceService,
                GoogleFitService(context, preferences),
                FitbitService(context, preferences),
                MisfitService(context, preferences),
                RescueTimeService(context, preferences)
                //,MicrosoftBandService
                //,MiBandService
        )
    }


    private val availableServicesCache = ArrayList<OTExternalService>()
    private var isAvailableServiceCacheInvalid = true

    val availableServices: List<OTExternalService>
        @Synchronized get() {
            if (isAvailableServiceCacheInvalid) {
                availableServicesCache.clear()
                factoryCodeDict.clear()

                installedServices.filterTo(availableServicesCache) { service -> service.isSupportedInSystem(this) }

                for (service in availableServicesCache) {
                    service.initialize()
                    for (factory in service.measureFactories) {
                        factoryCodeDict.put(factory.typeCode, factory)
                    }
                }
            }

            return availableServicesCache
        }

    val unSupportedDummyService: OTUnSupportedDummyService by lazy {
        OTUnSupportedDummyService(context, preferences)
    }

    val unSupportedDummyMeasureFactory: OTUnSupportedDummyService.OTUnSupportedDummyMeasureFactory by lazy {
        OTUnSupportedDummyService.OTUnSupportedDummyMeasureFactory(context, unSupportedDummyService)
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
        if (BuildConfig.ENABLE_DYNAMIC_API_KEY_MODIFICATION == true) {
            val oldValue = getApiKey(key)
            preferences.edit().putString(key, value).apply()
            if (oldValue != value) {
                isAvailableServiceCacheInvalid = true
                LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(BROADCAST_ACTION_SERVICE_API_KEYS_CHANGED))
            }
        }
    }

    fun removeApiKeyFromLocal(key: String) {
        if (BuildConfig.ENABLE_DYNAMIC_API_KEY_MODIFICATION == true) {
            val oldValue = getApiKey(key)
            if (oldValue != null) {
                preferences.edit().remove(key).apply()
                isAvailableServiceCacheInvalid = true
                LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(BROADCAST_ACTION_SERVICE_API_KEYS_CHANGED))
            }
        }
    }

    fun getApiKey(key: String): String? {
        return if (BuildConfig.ENABLE_DYNAMIC_API_KEY_MODIFICATION == true && preferences.contains(key)) {
            getApiKeyInLocal(key)
        } else {
            getApiKeyInBuildConfig(key)
        }
    }

    fun getApiKeyInBuildConfig(key: String): String? {
        return BuildConfig.apiKeyTable.get(key)
    }

    fun getApiKeyInLocal(key: String): String? {
        return preferences.getString(key, null)
    }
}