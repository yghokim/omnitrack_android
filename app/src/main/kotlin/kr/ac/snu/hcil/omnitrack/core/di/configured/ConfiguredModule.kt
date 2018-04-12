package kr.ac.snu.hcil.omnitrack.core.di.configured

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.configuration.OTConfiguration
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import java.io.File
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
    @ConfiguredObject
    fun providesConfiguredPreferences(context: Context, configuration: OTConfiguration): SharedPreferences {
        return context.getSharedPreferences(configuration.id, Context.MODE_PRIVATE)
    }

    @Provides
    @Configured
    fun providesConfiguredContext(): ConfiguredContext {
        return parent
    }

    @Provides
    @Configured
    @ConfigurationDirectory
    fun providesConfigurationDirectory(context: Context, configuration: OTConfiguration): File {
        return File(context.filesDir, configuration.id)
    }

    @Provides
    fun providesPreferredTimeZone(): TimeZone {
        return TimeZone.getDefault()
    }
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ConfigurationDirectory
