package kr.ac.snu.hcil.omnitrack.core.di.global

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import javax.inject.Qualifier
import javax.inject.Singleton

@Module(includes = [ApplicationModule::class])
class ExternalServiceModule {

    @Provides
    @Singleton
    @ExternalService
    fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("ExternalServices", Context.MODE_PRIVATE)
    }

}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ExternalService