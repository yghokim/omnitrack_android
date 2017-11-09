package kr.ac.snu.hcil.omnitrack.core.di

import dagger.Subcomponent
import kr.ac.snu.hcil.omnitrack.services.OTItemLoggingService
import kr.ac.snu.hcil.omnitrack.services.OTSynchronizationService
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeScreenViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.home.OrderedTrackerListViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.home.SidebarWrapper
import kr.ac.snu.hcil.omnitrack.ui.pages.home.TrackerListViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemEditionViewModelBase
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemListViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.items.NewItemCreationViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.tracker.TrackerDetailViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.TriggerDetailViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.AManagedTriggerListViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.TriggerViewModel

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

    fun inject(viewModel: HomeScreenViewModel)
    fun inject(service: OTSynchronizationService)
    fun inject(sidebar: SidebarWrapper)
    fun inject(viewModel: OrderedTrackerListViewModel)
    fun inject(viewModel: TrackerDetailViewModel)
    fun inject(viewModel: TrackerListViewModel)
    fun inject(viewModel: ItemListViewModel)
    fun inject(viewModel: AManagedTriggerListViewModel)
    fun inject(viewModel: ItemEditionViewModelBase)
    fun inject(service: OTItemLoggingService)
    fun inject(viewModel: TriggerDetailViewModel)
    fun inject(viewModel: TriggerViewModel)
    fun inject(viewModel: NewItemCreationViewModel)
}