package kr.ac.snu.hcil.omnitrack.core.di

import com.google.gson.Gson
import dagger.Component
import dagger.Lazy
import dagger.Module
import dagger.Provides
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.database.local.*
import kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters.*
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerSystemManager
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Created by younghokim on 2017-11-02.
 */
@Module(includes = arrayOf(TriggerSystemModule::class))
class DaoSerializationModule {

    @Provides
    @Singleton
    @ForGeneric
    fun provideGenericGson(): Gson
    {
        return Gson()
    }

    @Provides
    @Singleton
    @ForAttribute
    fun provideAttributeAdapter(@ForGeneric gson: Lazy<Gson>): ServerCompatibleTypeAdapter<OTAttributeDAO> = AttributeTypeAdapter(false, gson)

    @Provides
    @Singleton
    @ForServerAttribute
    fun provideServerAttributeAdapter(@ForGeneric gson: Lazy<Gson>): ServerCompatibleTypeAdapter<OTAttributeDAO> = AttributeTypeAdapter(true, gson)

    @Provides
    @Singleton
    @ForTrigger
    fun provideTriggerAdapter(@ForGeneric gson: Lazy<Gson>, realmProvider: Provider<Realm>, triggerSystemManager: Lazy<OTTriggerSystemManager>): ServerCompatibleTypeAdapter<OTTriggerDAO> = TriggerTypeAdapter(false, gson, realmProvider, triggerSystemManager)

    @Provides
    @Singleton
    @ForServerTrigger
    fun provideServerTriggerAdapter(@ForGeneric gson: Lazy<Gson>, realmProvider: Provider<Realm>, triggerSystemManager: Lazy<OTTriggerSystemManager>): ServerCompatibleTypeAdapter<OTTriggerDAO> = TriggerTypeAdapter(true, gson, realmProvider, triggerSystemManager)

    @Provides
    @Singleton
    @ForTracker
    fun provideTrackerAdapter(@ForAttribute attributeTypeAdapter: Lazy<ServerCompatibleTypeAdapter<OTAttributeDAO>>,
                              @ForGeneric gson: Lazy<Gson>): ServerCompatibleTypeAdapter<OTTrackerDAO>
            = TrackerTypeAdapter(false, attributeTypeAdapter, gson)


    @Provides
    @Singleton
    @ForServerTracker
    fun provideServerTrackerAdapter(@ForServerAttribute attributeTypeAdapter: Lazy<ServerCompatibleTypeAdapter<OTAttributeDAO>>,
                                    @ForGeneric gson: Lazy<Gson>): ServerCompatibleTypeAdapter<OTTrackerDAO>
            = TrackerTypeAdapter(true, attributeTypeAdapter, gson)



    @Provides
    @Singleton
    @ForItem
    fun provideItemAdapter(): ServerCompatibleTypeAdapter<OTItemDAO>
            = ItemTypeAdapter(false)


    @Provides
    @Singleton
    @ForServerItem
    fun provideServerItemAdapter(): ServerCompatibleTypeAdapter<OTItemDAO>
            = ItemTypeAdapter(true)
}

@Singleton
@Component(modules = arrayOf(DaoSerializationModule::class, BackendDatabaseModule::class), dependencies = arrayOf())
interface DaoSerializationComponent {

    fun manager(): Lazy<DaoSerializationManager>
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ForGeneric

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ForTrigger

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ForTracker

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ForAttribute

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ForItem

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ForServerTrigger

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ForServerTracker

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ForServerAttribute

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ForServerItem