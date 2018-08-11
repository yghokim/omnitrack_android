package kr.ac.snu.hcil.omnitrack.core.di.configured

import android.content.Context
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.configuration.OTConfiguration
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.utils.time.LocalTimeFormats
import java.util.*

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

    @Provides
    fun providesLocalTimeFormats(context: Context): LocalTimeFormats {
        return LocalTimeFormats(context)
    }
}
