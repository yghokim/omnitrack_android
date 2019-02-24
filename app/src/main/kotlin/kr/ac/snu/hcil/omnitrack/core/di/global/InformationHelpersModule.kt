package kr.ac.snu.hcil.omnitrack.core.di.global

import android.content.Context
import dagger.Lazy
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import javax.inject.Singleton

/**
 * Created by Young-Ho on 11/3/2017.
 */
@Module(includes = [AuthModule::class, ApplicationModule::class])
class InformationHelpersModule {

    @Provides
    @Singleton
    fun getAttributeManager(context: Context, authManager: Lazy<OTAuthManager>): OTAttributeManager {
        return OTAttributeManager(context, authManager)
    }

    @Provides
    @Singleton
    fun getPropertyManager(context: Context): OTPropertyManager {
        return OTPropertyManager(context)
    }
}