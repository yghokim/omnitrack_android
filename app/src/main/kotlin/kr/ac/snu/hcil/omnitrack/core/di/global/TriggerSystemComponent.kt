package kr.ac.snu.hcil.omnitrack.core.di.global

import dagger.Component
import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.core.triggers.ITriggerAlarmController
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerSystemManager
import kr.ac.snu.hcil.omnitrack.receivers.TimeTriggerAlarmReceiver
import kr.ac.snu.hcil.omnitrack.services.OTReminderService
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.TimeConditionViewModel
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 11. 9..
 */
@Singleton
@Component(modules = [TriggerSystemModule::class, BackendDatabaseModule::class])
interface TriggerSystemComponent {


    fun getTriggerSystemManager(): Lazy<OTTriggerSystemManager>

    fun getTriggerAlarmController(): ITriggerAlarmController

    fun inject(alarmService: TimeTriggerAlarmReceiver.TimeTriggerWakefulHandlingService)
    fun inject(viewModel: TimeConditionViewModel)
    fun inject(service: OTReminderService)
}