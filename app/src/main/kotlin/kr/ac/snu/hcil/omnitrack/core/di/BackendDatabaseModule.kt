package kr.ac.snu.hcil.omnitrack.core.di

import dagger.Module
import dagger.Provides
import dagger.internal.Factory
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.annotations.RealmModule
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.core.database.local.models.*
import kr.ac.snu.hcil.omnitrack.core.database.local.models.helpermodels.*
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Created by Young-Ho on 11/3/2017.
 */

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class Backend

@Module(includes = arrayOf(AuthModule::class, NetworkModule::class, DaoSerializationModule::class))
class BackendDatabaseModule {

    @Provides
    @Singleton
    @Backend
    fun backendDatabaseConfiguration(): RealmConfiguration {
        return RealmConfiguration.Builder()
                .name("backend.db")
                .modules(BackendRealmModule())
                .run {
                    if (BuildConfig.DEBUG) {
                        this.deleteRealmIfMigrationNeeded()
                    } else this
                }
                .build()
    }

    @Provides
    @Singleton
    @Backend
    fun makeBackendDbRealmProvider(@Backend configuration: RealmConfiguration): Factory<Realm> {
        return object : Factory<Realm> {
            override fun get(): Realm {
                return Realm.getInstance(configuration)
            }

        }
    }
}

@RealmModule(classes = arrayOf(
        OTUserDAO::class,
        OTTrackerDAO::class,
        OTAttributeDAO::class,
        OTTriggerDAO::class,
        OTItemDAO::class,
        OTItemValueEntryDAO::class,
        OTItemBuilderFieldValueEntry::class,
        OTItemBuilderDAO::class,
        OTItemBuilderFieldValueEntry::class,
        OTStringStringEntryDAO::class,
        OTIntegerStringEntryDAO::class,
        OTTriggerAlarmInstance::class,
        OTTriggerReminderEntry::class,
        OTTriggerSchedule::class,
        UploadTaskInfo::class
))
class BackendRealmModule
