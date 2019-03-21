package kr.ac.snu.hcil.omnitrack.core.di.global

import com.google.gson.Gson
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.typeadapters.*
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import kr.ac.snu.hcil.omnitrack.core.system.OTMeasureFactoryManager
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTDataDrivenTriggerCondition
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Created by younghokim on 2017-11-02.
 */
@Module(includes = [SerializationModule::class])
class DaoSerializationModule {

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
    fun provideTriggerAdapter(@ForGeneric gson: Lazy<Gson>, @Backend realmProvider: Factory<Realm>): ServerCompatibleTypeAdapter<OTTriggerDAO> = TriggerTypeAdapter(false, gson, realmProvider)

    @Provides
    @Singleton
    @ForServerTrigger
    fun provideServerTriggerAdapter(@ForGeneric gson: Lazy<Gson>, @Backend realmProvider: Factory<Realm>): ServerCompatibleTypeAdapter<OTTriggerDAO> = TriggerTypeAdapter(true, gson, realmProvider)

    @Provides
    @Singleton
    @ForTracker
    fun provideTrackerAdapter(@ForAttribute attributeTypeAdapter: Lazy<ServerCompatibleTypeAdapter<OTAttributeDAO>>,
                              @ForGeneric gson: Lazy<Gson>): ServerCompatibleTypeAdapter<OTTrackerDAO> = TrackerTypeAdapter(false, attributeTypeAdapter, gson)


    @Provides
    @Singleton
    @ForServerTracker
    fun provideServerTrackerAdapter(@ForServerAttribute attributeTypeAdapter: Lazy<ServerCompatibleTypeAdapter<OTAttributeDAO>>,
                                    @ForGeneric gson: Lazy<Gson>): ServerCompatibleTypeAdapter<OTTrackerDAO> = TrackerTypeAdapter(true, attributeTypeAdapter, gson)

    @Provides
    @Singleton
    @ForItem
    fun provideItemAdapter(@ForGeneric gson: Lazy<Gson>): ServerCompatibleTypeAdapter<OTItemDAO> = ItemTypeAdapter(false, gson)


    @Provides
    @Singleton
    @ForServerItem
    fun provideServerItemAdapter(@ForGeneric gson: Lazy<Gson>): ServerCompatibleTypeAdapter<OTItemDAO> = ItemTypeAdapter(true, gson)


    @Provides
    @Singleton
    fun provideTimeRangeQueryTypeAdapter(): OTTimeRangeQuery.TimeRangeQueryTypeAdapter {
        return OTTimeRangeQuery.TimeRangeQueryTypeAdapter()
    }

    @Provides
    @Singleton
    fun provideConnectionTypeAdapter(measureFactoryManager: OTMeasureFactoryManager, timeQueryRangeQueryTypeAdapter: Lazy<OTTimeRangeQuery.TimeRangeQueryTypeAdapter>): OTConnection.ConnectionTypeAdapter {
        return OTConnection.ConnectionTypeAdapter(measureFactoryManager, timeQueryRangeQueryTypeAdapter)
    }

    @Provides
    @Singleton
    fun provideDataDrivenConnectionTypeAdapter(serviceManager: OTExternalServiceManager, timeQueryRangeQueryTypeAdapter: Lazy<OTTimeRangeQuery.TimeRangeQueryTypeAdapter>): OTDataDrivenTriggerCondition.ConditionTypeAdapter {
        return OTDataDrivenTriggerCondition.ConditionTypeAdapter(serviceManager, timeQueryRangeQueryTypeAdapter)
    }

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
@Retention(AnnotationRetention.RUNTIME)
annotation class ForValueConnection

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ForServerTrigger

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ForServerTracker

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ForServerAttribute

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ForServerItem