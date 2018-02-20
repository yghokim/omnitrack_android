package kr.ac.snu.hcil.omnitrack.services

import com.firebase.jobdispatcher.JobParameters
import io.reactivex.disposables.CompositeDisposable
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.research.ResearchManager
import kr.ac.snu.hcil.omnitrack.utils.ConfigurableJobService
import javax.inject.Inject

/**
 * Created by younghokim on 2018. 2. 20..
 */
class OTResearchSynchronizationService : ConfigurableJobService() {

    inner class ConfiguredTask(configuredContext: ConfiguredContext) : IConfiguredTask {

        private val subscriptions = CompositeDisposable()

        @Inject
        lateinit var researchManager: ResearchManager

        init {
            configuredContext.researchComponent.inject(this)
        }

        override fun dispose() {
            subscriptions.clear()
        }

        override fun onStartJob(job: JobParameters): Boolean {
            subscriptions.add(
                    researchManager.updateExperimentsFromServer().subscribe({
                        jobFinished(job, false)
                    }, {
                        it.printStackTrace()
                        jobFinished(job, true)
                    })
            )
            return true
        }

        override fun onStopJob(job: JobParameters): Boolean {
            dispose()
            return true
        }

    }

    override fun makeNewTask(configuredContext: ConfiguredContext): IConfiguredTask {
        return ConfiguredTask(configuredContext)
    }

}