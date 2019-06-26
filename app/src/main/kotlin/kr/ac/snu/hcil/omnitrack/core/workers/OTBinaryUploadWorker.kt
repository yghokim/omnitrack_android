package kr.ac.snu.hcil.omnitrack.core.workers

import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTBinaryUploadCommands
import java.util.concurrent.Executors

/**
 * Created by younghokim on 2017. 9. 26..
 */
class OTBinaryUploadWorker(val context: Context, workerParams: WorkerParameters) : RxWorker(context, workerParams) {

    override fun getBackgroundScheduler(): Scheduler {
        return realmScheduler
    }

    private val realmScheduler = Schedulers.from(Executors.newSingleThreadExecutor())

    init {
    }

    override fun createWork(): Single<Result> {
        return OTBinaryUploadCommands(context).createWork().toSingle { Result.success() }
                .onErrorReturn {
                    it.printStackTrace()
                    Result.retry()
                }
    }
}