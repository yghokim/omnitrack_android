package kr.ac.snu.hcil.omnitrack.core.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.di.ExternalServiceModule
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import kr.ac.snu.hcil.omnitrack.core.triggers.*
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 11. 9..
 */
@Module(includes = [ExternalServiceModule::class])
class TriggerSystemModule {
    @Provides
    @Singleton
    fun provideTriggerAlarmController(context: Context, @Backend realmProvider: Factory<Realm>): ITriggerAlarmController {
        return OTTriggerAlarmManager(context, realmProvider)
    }

    @Provides
    @Singleton
    fun provideDataDrivenTriggerManager(@Default preferences: Lazy<SharedPreferences>, context: Context, externalServiceManager: Lazy<OTExternalServiceManager>, @Backend realmFactory: Factory<Realm>): OTDataDrivenTriggerManager {
        return OTDataDrivenTriggerManager(context, preferences, externalServiceManager, realmFactory)
    }

    @Provides
    @Singleton
    fun provideTriggerSystemManager(
            triggerAlarmManager: Lazy<ITriggerAlarmController>,
            dataDrivenTriggerManager: Lazy<OTDataDrivenTriggerManager>,
            eventTriggerManager: Lazy<OTEventTriggerManager>,
            @Backend realmProvider: Factory<Realm>,
            context: Context): OTTriggerSystemManager {
        return OTTriggerSystemManager(triggerAlarmManager, dataDrivenTriggerManager, eventTriggerManager, realmProvider, context)
    }

    @Provides
    @Singleton
    fun provideEventTriggerManager(): OTEventTriggerManager{
        return OTEventTriggerManager()
    }
}