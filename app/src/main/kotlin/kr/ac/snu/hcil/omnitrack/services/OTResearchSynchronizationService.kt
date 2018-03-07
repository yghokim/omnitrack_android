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
                        println("successfully received the experiment informations from server.")
                        jobFinished(job, false)
                    }, {
                        it.printStackTrace()
                        println("Failed to receive the experiment informations from server. Retry later.")
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

    override fun extractConfigIdOfJob(job: JobParameters): String {
        return job.tag.split(";").last()
    }

    override fun makeNewTask(configuredContext: ConfiguredContext): IConfiguredTask {
        return ConfiguredTask(configuredContext)
    }

}