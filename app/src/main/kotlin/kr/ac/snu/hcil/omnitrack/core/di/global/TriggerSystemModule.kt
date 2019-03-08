package kr.ac.snu.hcil.omnitrack.core.di.global

import android.content.Context
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import kr.ac.snu.hcil.omnitrack.core.triggers.ITriggerAlarmController
import kr.ac.snu.hcil.omnitrack.core.triggers.OTDataDrivenTriggerManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerAlarmManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerSystemManager
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
    fun provideDataDrivenTriggerManager(context: Context, externalServiceManager: Lazy<OTExternalServiceManager>, @Backend realmFactory: Factory<Realm>): OTDataDrivenTriggerManager {
        return OTDataDrivenTriggerManager(context, externalServiceManager, realmFactory)
    }

    @Provides
    @Singleton
    fun provideTriggerSystemManager(triggerAlarmManager: Lazy<ITriggerAlarmController>, dataDrivenTriggerManager: Lazy<OTDataDrivenTriggerManager>, @Backend realmProvider: Factory<Realm>, context: Context): OTTriggerSystemManager {
        return OTTriggerSystemManager(triggerAlarmManager, dataDrivenTriggerManager, realmProvider, context)
    }
}