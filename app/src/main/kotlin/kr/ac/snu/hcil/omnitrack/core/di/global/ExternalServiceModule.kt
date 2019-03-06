package kr.ac.snu.hcil.omnitrack.core.di.global

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.Reusable
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import javax.inject.Qualifier

@Module(includes = [ApplicationModule::class])
class ExternalServiceModule {

    private var serviceManager: OTExternalServiceManager? = null

    init {
        println("externalService module was created: ${this}")
    }

    @Provides
    @Reusable
    @ExternalService
    fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("ExternalServices", Context.MODE_PRIVATE)
    }

    @Provides
    @Reusable
    fun provideExternalServiceManager(context: Context, @ExternalService prefs: SharedPreferences): OTExternalServiceManager {
        if (serviceManager == null) serviceManager = OTExternalServiceManager(context, prefs)
        return serviceManager!!
    }

}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ExternalService