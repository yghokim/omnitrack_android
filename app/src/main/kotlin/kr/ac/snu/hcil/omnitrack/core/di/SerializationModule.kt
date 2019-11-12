package kr.ac.snu.hcil.omnitrack.core.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 12. 19..
 */
@Module()
class SerializationModule {
    @Provides
    @Singleton
    @ForGeneric
    fun provideGenericGson(): Gson {
        return Gson()
    }

}

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ForGeneric