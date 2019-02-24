package kr.ac.snu.hcil.omnitrack.core.di.global

import dagger.Component
import kr.ac.snu.hcil.omnitrack.services.OTResearchSynchronizationWorker
import kr.ac.snu.hcil.omnitrack.ui.pages.research.ResearchViewModel
import javax.inject.Singleton

/**
 * Created by younghokim on 2018. 1. 3..
 */
@Singleton
@Component(modules = [ResearchModule::class, AuthModule::class, BackendDatabaseModule::class, TriggerSystemModule::class])
interface ResearchComponent {

    fun inject(viewModel: ResearchViewModel)
    fun inject(service: OTResearchSynchronizationWorker)
}