package kr.ac.snu.hcil.omnitrack.core.di

import android.content.Context
import dagger.Lazy
import dagger.Module
import dagger.Provides
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.triggers.ITriggerAlarmController
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerAlarmManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerSystemManager
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 11. 9..
 */
@Singleton
@Module(includes = arrayOf(ApplicationModule::class, ScheduledJobModule::class))
class TriggerSystemModule {
    @Provides
    @Singleton
    fun provideTriggerAlarmController(context: Context, realmProvider: Provider<Realm>): ITriggerAlarmController {
        return OTTriggerAlarmManager(context, realmProvider)
    }

    @Provides
    @Singleton
    fun provideTriggerSystemManager(triggerAlarmManager: Lazy<ITriggerAlarmController>): OTTriggerSystemManager {
        return OTTriggerSystemManager(triggerAlarmManager)
    }
}