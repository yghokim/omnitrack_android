package kr.ac.snu.hcil.omnitrack.core.di.global

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import javax.inject.Qualifier
import javax.inject.Singleton

@Module(includes = [ApplicationModule::class])
class ExternalServiceModule {

    init {
        println("externalService module was created: ${this}")
    }

    @Provides
    @Singleton
    @ExternalService
    fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("ExternalServices", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideExternalServiceManager(context: Context, @ExternalService prefs: SharedPreferences): OTExternalServiceManager {
        return OTExternalServiceManager(context, prefs)
    }

}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ExternalService