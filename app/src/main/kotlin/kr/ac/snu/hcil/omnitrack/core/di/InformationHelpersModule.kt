package kr.ac.snu.hcil.omnitrack.core.di

import dagger.Lazy
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import javax.inject.Singleton

/**
 * Created by Young-Ho on 11/3/2017.
 */
@Module(includes = arrayOf(AuthModule::class))
class InformationHelpersModule {

    @Provides
    @Singleton
    fun getAttributeManager(authManager: Lazy<OTAuthManager>): OTAttributeManager {
        return OTAttributeManager(authManager)
    }
}