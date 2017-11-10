package kr.ac.snu.hcil.omnitrack.core.di

import dagger.Lazy
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTimeTriggerAlarmManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerSystemManager
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 11. 9..
 */
@Singleton
@Module(includes = arrayOf(ScheduledJobModule::class))
class TriggerSystemModule {
    @Provides
    @Singleton
    fun provideTimeTriggerManager(): OTTimeTriggerAlarmManager {
        return OTTimeTriggerAlarmManager()
    }

    @Provides
    @Singleton
    fun provideTriggerSystemManager(timeTriggerAlarmManager: Lazy<OTTimeTriggerAlarmManager>): OTTriggerSystemManager {
        return OTTriggerSystemManager(timeTriggerAlarmManager)
    }
}