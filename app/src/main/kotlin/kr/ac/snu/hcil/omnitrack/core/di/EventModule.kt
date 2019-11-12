package kr.ac.snu.hcil.omnitrack.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.system.OTEventFactoryManager
import javax.inject.Singleton

@Module(includes = [ApplicationModule::class])
class EventModule {

    @Provides
    @Singleton
    fun provideEventFactoryManager(context: Context): OTEventFactoryManager {
        return OTEventFactoryManager(context)
    }

}