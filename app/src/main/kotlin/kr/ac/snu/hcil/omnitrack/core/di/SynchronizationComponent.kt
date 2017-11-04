package kr.ac.snu.hcil.omnitrack.core.di

import dagger.Subcomponent
import kr.ac.snu.hcil.omnitrack.services.OTSynchronizationService

/**
 * Created by younghokim on 2017. 11. 4..
 */
@ApplicationScope
@Subcomponent(modules = arrayOf(SynchronizationModule::class, ScheduledJobModule::class))
interface SynchronizationComponent {
    @Subcomponent.Builder
    interface Builder {
        fun setMainModule(module: SynchronizationModule): Builder
        fun setScheduledJobModule(module: ScheduledJobModule): Builder
        fun build(): SynchronizationComponent
    }


    fun inject(service: OTSynchronizationService)
}