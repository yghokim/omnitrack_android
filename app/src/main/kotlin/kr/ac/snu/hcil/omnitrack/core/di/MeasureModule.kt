package kr.ac.snu.hcil.omnitrack.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.di.ExternalServiceModule
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import kr.ac.snu.hcil.omnitrack.core.system.OTItemDynamicMeasureFactoryManager
import kr.ac.snu.hcil.omnitrack.core.system.OTMeasureFactoryManager
import javax.inject.Singleton


@Module(includes = [ApplicationModule::class, ExternalServiceModule::class])
class MeasureModule {

    @Provides
    @Singleton
    fun provideOTItemDynamicMeasureFactoryManager(context: Context): OTItemDynamicMeasureFactoryManager {
        return OTItemDynamicMeasureFactoryManager(context)
    }

    @Provides
    @Singleton
    fun provideMeasureFactoryManager(serviceManager: OTExternalServiceManager, itemMeasureFactoryManager: OTItemDynamicMeasureFactoryManager): OTMeasureFactoryManager {
        return OTMeasureFactoryManager(serviceManager, itemMeasureFactoryManager)
    }
}