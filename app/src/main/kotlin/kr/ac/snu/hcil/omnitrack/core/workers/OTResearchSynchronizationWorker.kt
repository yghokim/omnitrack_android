package kr.ac.snu.hcil.omnitrack.core.workers

import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.research.ResearchManager
import javax.inject.Inject

/**
 * Created by younghokim on 2018. 2. 20..
 */
class OTResearchSynchronizationWorker(context: Context, workerParams: WorkerParameters) : RxWorker(context, workerParams) {

    companion object {
        const val TAG = "OTResearchSynchronizationWorker"
    }

    @Inject
    lateinit var researchManager: ResearchManager

    init {
        (context.applicationContext as OTAndroidApp).researchComponent.inject(this)
    }

    override fun createWork(): Single<Result> {
        return researchManager.updateExperimentsFromServer().toSingle {
            println("successfully received the experiment information from server.")
            Result.success()
        }.onErrorReturn {
            it.printStackTrace()
            println("Failed to receive the experiment informations from server. Retry later.")
            Result.retry()
        }
    }
}