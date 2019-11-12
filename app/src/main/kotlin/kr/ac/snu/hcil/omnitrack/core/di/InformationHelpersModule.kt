package kr.ac.snu.hcil.omnitrack.core.di

import android.content.Context
import dagger.Lazy
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.fields.OTFieldManager
import kr.ac.snu.hcil.omnitrack.core.fields.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import javax.inject.Singleton

/**
 * Created by Young-Ho on 11/3/2017.
 */
@Module(includes = [AuthModule::class, ApplicationModule::class])
class InformationHelpersModule {

    @Provides
    @Singleton
    fun getAttributeManager(context: Context, authManager: Lazy<OTAuthManager>): OTFieldManager {
        return OTFieldManager(context, authManager)
    }

    @Provides
    @Singleton
    fun getPropertyManager(context: Context): OTPropertyManager {
        return OTPropertyManager(context)
    }
}