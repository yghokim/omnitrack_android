package kr.ac.snu.hcil.omnitrack.services

import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import io.reactivex.disposables.CompositeDisposable
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.research.ResearchManager
import javax.inject.Inject

/**
 * Created by younghokim on 2018. 2. 20..
 */
class OTResearchSynchronizationService : JobService() {


    private val subscriptions = CompositeDisposable()

    @Inject
    lateinit var researchManager: ResearchManager

    override fun onCreate() {
        super.onCreate()
        (application as OTAndroidApp).currentConfiguredContext.researchComponent.inject(this)
    }

    override fun onDestroy() {
        super.onDestroy()
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
        return true
    }
}