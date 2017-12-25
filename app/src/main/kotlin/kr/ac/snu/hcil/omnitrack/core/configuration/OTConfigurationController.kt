package kr.ac.snu.hcil.omnitrack.core.configuration

import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.database.global.OTAttachedConfigurationDao
import kr.ac.snu.hcil.omnitrack.core.di.global.AppLevelDatabase
import kr.ac.snu.hcil.omnitrack.core.di.global.ApplicationComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 12. 17..
 */
@Singleton
class OTConfigurationController @Inject constructor(
        val applicationComponent: ApplicationComponent,
        @AppLevelDatabase val appLevelRealmFactory: Factory<Realm>
) {

    private lateinit var currentConfiguration: OTConfiguration

    val currentConfiguredContext: ConfiguredContext by lazy {
        ConfiguredContext(currentConfiguration, applicationComponent)
    }

    val configurationIterator: Iterator<OTConfiguration> by lazy {
        arrayOf(currentConfiguration).iterator()
    }

    init {
        appLevelRealmFactory.get().use { realm ->
            if (realm.where(OTAttachedConfigurationDao::class.java).count() == 0L) {
                //add default configuration
                currentConfiguration = OTConfiguration()
                realm.executeTransaction {
                    val dao = realm.createObject(OTAttachedConfigurationDao::class.java, currentConfiguration.id)
                    dao.dataJson = currentConfiguration.toJson()
                }
            } else {
                val configDao = realm.where(OTAttachedConfigurationDao::class.java).findFirst()!!
                currentConfiguration = configDao.staticConfiguration()
            }
        }
    }

    fun getConfiguredContextOf(config: OTConfiguration): ConfiguredContext? {
        return getConfiguredContextOf(config.id)
    }

    fun getConfiguredContextOf(configId: String): ConfiguredContext? {
        if (currentConfiguredContext.configuration.id == configId) {
            return currentConfiguredContext
        } else return null
    }

    fun <T> map(func: ((OTConfiguration) -> T)): List<T> {
        return listOf(currentConfiguration).map(func)
    }
}