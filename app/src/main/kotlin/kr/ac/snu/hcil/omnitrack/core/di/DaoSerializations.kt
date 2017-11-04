package kr.ac.snu.hcil.omnitrack.core.di

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import dagger.*
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.database.local.*
import kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters.AttributeTypeAdapter
import kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters.ItemTypeAdapter
import kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters.TrackerTypeAdapter
import kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters.TriggerTypeAdapter
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Created by younghokim on 2017-11-02.
 */
@Module
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
    fun provideAttributeAdapter(@ForGeneric gson: Lazy<Gson>): TypeAdapter<OTAttributeDAO> = AttributeTypeAdapter(gson)

    @Provides
    @Singleton
    @ForTrigger
    fun provideTriggerAdapter(realmProvider: Provider<Realm>): TypeAdapter<OTTriggerDAO> = TriggerTypeAdapter(realmProvider)

    @Provides
    @Singleton
    @ForTracker
    fun provideTrackerAdapter(@ForAttribute attributeTypeAdapter: Lazy<TypeAdapter<OTAttributeDAO>>, @ForGeneric gson: Lazy<Gson>): TypeAdapter<OTTrackerDAO>
            = TrackerTypeAdapter(attributeTypeAdapter, gson)

    @Provides
    @Singleton
    @ForItem
    fun provideItemAdapter(): TypeAdapter<OTItemDAO>
        = ItemTypeAdapter()
}

@Singleton
@Component(modules = arrayOf(DaoSerializationModule::class, BackendDatabaseModule::class))
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