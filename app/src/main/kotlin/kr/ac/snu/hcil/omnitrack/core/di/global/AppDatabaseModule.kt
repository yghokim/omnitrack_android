package kr.ac.snu.hcil.omnitrack.core.di.global

import dagger.Module
import dagger.Provides
import dagger.internal.Factory
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.annotations.RealmModule
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.core.database.global.OTAttachedConfigurationDao
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 12. 18..
 */
@Module()
class AppDatabaseModule {
    @Provides
    @Singleton
    @AppLevelDatabase
    fun providesRealmConfiguration(): RealmConfiguration {
        return RealmConfiguration.Builder()
                .name("common_backend.db")
                .modules(GlobalRealmModule())
                .run {
                    if (BuildConfig.DEBUG) {
                        this.deleteRealmIfMigrationNeeded()
                    } else this
                }
                .build()
    }

    @Provides
    @Singleton
    @AppLevelDatabase
    fun providesAppLevelRealmFactory(@AppLevelDatabase configuration: RealmConfiguration): Factory<Realm> {
        return Factory<Realm> {
            return@Factory Realm.getInstance(configuration)
        }
    }
}

@RealmModule(classes = [
    OTAttachedConfigurationDao::class
])
class GlobalRealmModule


@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class AppLevelDatabase

