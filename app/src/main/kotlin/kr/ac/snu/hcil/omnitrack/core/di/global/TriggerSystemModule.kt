package kr.ac.snu.hcil.omnitrack.core.di.global

import android.content.Context
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.triggers.ITriggerAlarmController
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerAlarmManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerSystemManager
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 11. 9..
 */
@Module()
class TriggerSystemModule {
    @Provides
    @Singleton
    fun provideTriggerAlarmController(context: Context, @Backend realmProvider: Factory<Realm>): ITriggerAlarmController {
        return OTTriggerAlarmManager(context, realmProvider)
    }

    @Provides
    @Singleton
    fun provideTriggerSystemManager(triggerAlarmManager: Lazy<ITriggerAlarmController>, @Backend realmProvider: Factory<Realm>, context: Context): OTTriggerSystemManager {
        return OTTriggerSystemManager(triggerAlarmManager, realmProvider, context)
    }
}