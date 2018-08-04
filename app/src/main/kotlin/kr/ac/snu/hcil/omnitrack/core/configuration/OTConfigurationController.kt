package kr.ac.snu.hcil.omnitrack.core.configuration

import kr.ac.snu.hcil.omnitrack.core.di.global.ApplicationComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 12. 17..
 */
@Singleton
class OTConfigurationController @Inject constructor(
        val applicationComponent: ApplicationComponent
) {

    private val currentConfiguration: OTConfiguration = OTConfiguration()

    val currentConfiguredContext: ConfiguredContext by lazy {
        ConfiguredContext(currentConfiguration, applicationComponent)
    }

    fun <T> map(func: ((OTConfiguration) -> T)): List<T> {
        return listOf(currentConfiguration).map(func)
    }
}