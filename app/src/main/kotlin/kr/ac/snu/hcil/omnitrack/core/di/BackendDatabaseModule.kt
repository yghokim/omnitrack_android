package kr.ac.snu.hcil.omnitrack.core.di

import dagger.Module
import dagger.Provides
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.annotations.RealmModule
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.core.database.local.models.*
import kr.ac.snu.hcil.omnitrack.core.database.local.models.helpermodels.*
import javax.inject.Singleton

/**
 * Created by Young-Ho on 11/3/2017.
 */
@Module(includes = arrayOf(AuthModule::class, NetworkModule::class, DaoSerializationModule::class))
class BackendDatabaseModule {

    @Provides
    @Singleton
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
    fun makeBackendDbRealm(configuration: RealmConfiguration): Realm {
        return Realm.getInstance(configuration)
    }
}

@RealmModule(classes = arrayOf(
        OTTrackerDAO::class,
        OTAttributeDAO::class,
        OTTriggerDAO::class,
        OTItemDAO::class,
        OTItemValueEntryDAO::class,
        OTItemBuilderFieldValueEntry::class,
        LocalMediaCacheEntry::class,
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