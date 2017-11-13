package kr.ac.snu.hcil.omnitrack.core.di

import dagger.Component
import kr.ac.snu.hcil.omnitrack.receivers.TimeTriggerAlarmReceiver
import kr.ac.snu.hcil.omnitrack.services.OTReminderService
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.TimeConditionViewModel
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 11. 9..
 */
@Singleton
@Component(modules = arrayOf(TriggerSystemModule::class, BackendDatabaseModule::class))
interface TriggerSystemComponent {

    fun inject(alarmService: TimeTriggerAlarmReceiver.TimeTriggerWakefulHandlingService)
    fun inject(viewModel: TimeConditionViewModel)
    fun inject(service: OTReminderService)
}