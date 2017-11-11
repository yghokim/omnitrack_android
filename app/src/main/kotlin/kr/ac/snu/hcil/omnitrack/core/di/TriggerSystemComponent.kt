package kr.ac.snu.hcil.omnitrack.core.di

import dagger.Component
import kr.ac.snu.hcil.omnitrack.receivers.TimeTriggerAlarmReceiver
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 11. 9..
 */
@Singleton
@Component(modules = arrayOf(TriggerSystemModule::class))
interface TriggerSystemComponent {
    fun inject(alarmService: TimeTriggerAlarmReceiver.TimeTriggerWakefulHandlingService)
}