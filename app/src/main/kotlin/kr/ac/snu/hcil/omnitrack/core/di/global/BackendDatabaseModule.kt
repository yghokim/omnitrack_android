package kr.ac.snu.hcil.omnitrack.core.di.global

import dagger.Module
import dagger.Provides
import dagger.internal.Factory
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.annotations.RealmModule
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.core.database.BackendRealmMigration
import kr.ac.snu.hcil.omnitrack.core.database.models.*
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.*
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Created by Young-Ho on 11/3/2017.
 */

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Backend

@Module(includes = [AuthModule::class, NetworkModule::class, DaoSerializationModule::class])
class BackendDatabaseModule {

    @Provides
    @Singleton
    @Backend
    fun backendDatabaseConfiguration(): RealmConfiguration {
        return RealmConfiguration.Builder()
                .name("backend.db")
                .modules(BackendRealmModule())
                .schemaVersion(1)
                .apply {
                    if (BuildConfig.DEBUG == true) {
                        //this.deleteRealmIfMigrationNeeded()
                    }
                }
                .migration(BackendRealmMigration())
                .build()
    }

    @Provides
    @Singleton
    @Backend
    fun makeBackendDbRealmProvider(@Backend configuration: RealmConfiguration): Factory<Realm> {
        return Factory { Realm.getInstance(configuration) }
    }
}

@RealmModule(classes = [
    OTTrackerDAO::class,
    OTFieldDAO::class,
    OTFieldValidatorDAO::class,
    OTDescriptionPanelDAO::class,
    OTTrackerLayoutElementDAO::class,
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
    OTTriggerMeasureEntry::class,
    OTTriggerMeasureHistoryEntry::class,
    UploadTaskInfo::class
])
class BackendRealmModule
