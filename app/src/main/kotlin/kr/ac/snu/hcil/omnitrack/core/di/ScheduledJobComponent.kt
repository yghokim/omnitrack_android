package kr.ac.snu.hcil.omnitrack.core.di

import dagger.Subcomponent
import kr.ac.snu.hcil.omnitrack.receivers.PackageReceiver
import kr.ac.snu.hcil.omnitrack.services.OTVersionCheckService
import kr.ac.snu.hcil.omnitrack.ui.pages.settings.SettingsActivity

/**
 * Created by Young-Ho on 11/3/2017.
 */
@ApplicationScope
@Subcomponent(modules = arrayOf(ScheduledJobModule::class))
interface ScheduledJobComponent {
    @Subcomponent.Builder
    interface Builder {
        fun setModule(module: ScheduledJobModule): Builder
        fun build(): ScheduledJobComponent
    }

    fun inject(service: OTVersionCheckService)
    fun inject(receiver: PackageReceiver)
    fun inject(fragment: SettingsActivity.SettingsFragment)
}