package kr.ac.snu.hcil.omnitrack.core.di.configured

import dagger.Lazy
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.di.Configured

/**
 * Created by Young-Ho on 11/3/2017.
 */
@Module(includes = [AuthModule::class])
class InformationHelpersModule {

    @Provides
    @Configured
    fun getAttributeManager(configuredContext: ConfiguredContext, authManager: Lazy<OTAuthManager>): OTAttributeManager {
        return OTAttributeManager(configuredContext, authManager)
    }

    @Provides
    @Configured
    fun getPropertyManager(configuredContext: ConfiguredContext): OTPropertyManager {
        return OTPropertyManager(configuredContext)
    }
}