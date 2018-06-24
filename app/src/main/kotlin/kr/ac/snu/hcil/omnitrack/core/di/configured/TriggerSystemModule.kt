package kr.ac.snu.hcil.omnitrack.core.di.configured

import android.content.Context
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.core.triggers.ITriggerAlarmController
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerAlarmManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerSystemManager

/**
 * Created by younghokim on 2017. 11. 9..
 */
@Module(includes = arrayOf(ScheduledJobModule::class))
class TriggerSystemModule {
    @Provides
    @Configured
    fun provideTriggerAlarmController(context: Context, configuredContext: ConfiguredContext, @Backend realmProvider: Factory<Realm>): ITriggerAlarmController {
        return OTTriggerAlarmManager(context, configuredContext, realmProvider)
    }

    @Provides
    @Configured
    fun provideTriggerSystemManager(triggerAlarmManager: Lazy<ITriggerAlarmController>, context: Context): OTTriggerSystemManager {
        return OTTriggerSystemManager(triggerAlarmManager, context)
    }
}