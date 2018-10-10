package kr.ac.snu.hcil.omnitrack.core.di.configured

import dagger.Lazy
import dagger.Subcomponent
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.core.triggers.ITriggerAlarmController
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerSystemManager
import kr.ac.snu.hcil.omnitrack.receivers.TimeTriggerAlarmReceiver
import kr.ac.snu.hcil.omnitrack.services.OTReminderService
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.TimeConditionViewModel

/**
 * Created by younghokim on 2017. 11. 9..
 */
@Configured
@Subcomponent(modules = [TriggerSystemModule::class, BackendDatabaseModule::class])
interface TriggerSystemComponent {

    @Subcomponent.Builder
    interface Builder {
        fun plus(module: TriggerSystemModule): Builder
        fun plus(module: BackendDatabaseModule): Builder
        fun plus(module: ConfiguredModule): Builder
        fun plus(module: AuthModule): Builder
        fun plus(module: NetworkModule): Builder
        fun build(): TriggerSystemComponent
    }

    fun getTriggerSystemManager(): Lazy<OTTriggerSystemManager>

    fun getTriggerAlarmController(): ITriggerAlarmController

    fun inject(alarmService: TimeTriggerAlarmReceiver.TimeTriggerWakefulHandlingService)
    fun inject(viewModel: TimeConditionViewModel)
    fun inject(service: OTReminderService)
}