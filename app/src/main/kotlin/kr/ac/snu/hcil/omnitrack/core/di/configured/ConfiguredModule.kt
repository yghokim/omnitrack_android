package kr.ac.snu.hcil.omnitrack.core.di.configured

import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.configuration.OTConfiguration
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import java.util.*
import javax.inject.Qualifier

/**
 * Created by Young-Ho on 12/9/2017.
 */
@Module()
class ConfiguredModule(val configuration: OTConfiguration, val parent: ConfiguredContext) {
    @Provides
    @Configured
    fun providesConfiguration(): OTConfiguration {
        return configuration
    }

    @Provides
    @Configured
    fun providesConfiguredContext(): ConfiguredContext {
        return parent
    }

    @Provides
    fun providesPreferredTimeZone(): TimeZone {
        return TimeZone.getDefault()
    }
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ConfigurationDirectory
