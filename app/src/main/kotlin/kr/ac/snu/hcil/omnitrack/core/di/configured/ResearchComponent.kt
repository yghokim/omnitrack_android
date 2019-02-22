package kr.ac.snu.hcil.omnitrack.core.di.configured

import dagger.Subcomponent
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.core.research.ResearchManager
import kr.ac.snu.hcil.omnitrack.services.OTResearchSynchronizationWorker
import kr.ac.snu.hcil.omnitrack.ui.pages.research.ResearchViewModel

/**
 * Created by younghokim on 2018. 1. 3..
 */
@Configured
@Subcomponent(modules = [ResearchModule::class, AuthModule::class, BackendDatabaseModule::class])
interface ResearchComponent {

    @Subcomponent.Builder
    interface Builder {
        fun plus(module: ConfiguredModule): Builder
        fun plus(module: ResearchModule): Builder
        fun plus(module: AuthModule): Builder
        fun plus(module: NetworkModule): Builder
        fun plus(module: BackendDatabaseModule): Builder
        fun build(): ResearchComponent
    }

    fun manager(): ResearchManager

    fun inject(viewModel: ResearchViewModel)
    fun inject(service: OTResearchSynchronizationWorker)
}