package kr.ac.snu.hcil.omnitrack.core.di

import dagger.Subcomponent
import kr.ac.snu.hcil.omnitrack.services.OTSynchronizationService
import kr.ac.snu.hcil.omnitrack.ui.pages.home.OrderedTrackerListViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.home.SidebarWrapper
import kr.ac.snu.hcil.omnitrack.ui.pages.tracker.TrackerDetailViewModel

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
    fun inject(sidebar: SidebarWrapper)
    fun inject(viewModel: OrderedTrackerListViewModel)
    fun inject(viewModel: TrackerDetailViewModel)
}