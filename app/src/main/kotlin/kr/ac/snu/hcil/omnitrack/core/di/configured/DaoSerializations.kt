package kr.ac.snu.hcil.omnitrack.core.di.configured

import com.google.gson.Gson
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.database.configured.DaoSerializationManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.typeadapters.*
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.core.di.global.ForGeneric
import javax.inject.Qualifier

/**
 * Created by younghokim on 2017-11-02.
 */
@Module()
class DaoSerializationModule {

    @Provides
    @Configured
    @ForAttribute
    fun provideAttributeAdapter(@ForGeneric gson: Lazy<Gson>): ServerCompatibleTypeAdapter<OTAttributeDAO> = AttributeTypeAdapter(false, gson)

    @Provides
    @Configured
    @ForServerAttribute
    fun provideServerAttributeAdapter(@ForGeneric gson: Lazy<Gson>): ServerCompatibleTypeAdapter<OTAttributeDAO> = AttributeTypeAdapter(true, gson)

    @Provides
    @Configured
    @ForTrigger
    fun provideTriggerAdapter(@ForGeneric gson: Lazy<Gson>, @Backend realmProvider: Factory<Realm>): ServerCompatibleTypeAdapter<OTTriggerDAO> = TriggerTypeAdapter(false, gson, realmProvider)

    @Provides
    @Configured
    @ForServerTrigger
    fun provideServerTriggerAdapter(@ForGeneric gson: Lazy<Gson>, @Backend realmProvider: Factory<Realm>): ServerCompatibleTypeAdapter<OTTriggerDAO> = TriggerTypeAdapter(true, gson, realmProvider)

    @Provides
    @Configured
    @ForTracker
    fun provideTrackerAdapter(@ForAttribute attributeTypeAdapter: Lazy<ServerCompatibleTypeAdapter<OTAttributeDAO>>,
                              @ForGeneric gson: Lazy<Gson>): ServerCompatibleTypeAdapter<OTTrackerDAO>
            = TrackerTypeAdapter(false, attributeTypeAdapter, gson)


    @Provides
    @Configured
    @ForServerTracker
    fun provideServerTrackerAdapter(@ForServerAttribute attributeTypeAdapter: Lazy<ServerCompatibleTypeAdapter<OTAttributeDAO>>,
                                    @ForGeneric gson: Lazy<Gson>): ServerCompatibleTypeAdapter<OTTrackerDAO>
            = TrackerTypeAdapter(true, attributeTypeAdapter, gson)



    @Provides
    @Configured
    @ForItem
    fun provideItemAdapter(): ServerCompatibleTypeAdapter<OTItemDAO>
            = ItemTypeAdapter(false)


    @Provides
    @Configured
    @ForServerItem
    fun provideServerItemAdapter(): ServerCompatibleTypeAdapter<OTItemDAO>
            = ItemTypeAdapter(true)
}

@Configured
@Subcomponent(modules = arrayOf(DaoSerializationModule::class, BackendDatabaseModule::class))
interface DaoSerializationComponent {

    @Subcomponent.Builder
    interface Builder {

        fun plus(module: DaoSerializationModule): Builder
        fun plus(module: BackendDatabaseModule): Builder
        fun plus(module: AuthModule): Builder
        fun plus(module: ConfiguredModule): Builder


        fun build(): DaoSerializationComponent
    }

    fun manager(): DaoSerializationManager
}


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