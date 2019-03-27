package kr.ac.snu.hcil.omnitrack.core.workers

import android.content.Context
import android.util.Log
import androidx.work.RxWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.UsageLog
import kr.ac.snu.hcil.omnitrack.core.di.global.UsageLogger
import kr.ac.snu.hcil.omnitrack.core.net.IUsageLogUploadAPI
import java.util.concurrent.Executors
import javax.inject.Inject


/**
 * Created by younghokim on 2017. 11. 28..
 */

class OTUsageLogUploadWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    companion object {
        const val TAG = "OTUsageLogUploadWorker"
    }

    @field:[Inject UsageLogger]
    lateinit var realmFactory: Factory<Realm>

    @Inject
    lateinit var usageLogUploadApi: Lazy<IUsageLogUploadAPI>

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    override fun doWork(): Result {
        val realm = realmFactory.get()

        val pendingUsageLogCount = realm.where(UsageLog::class.java).equalTo("isSynchronized", false).count()
        if (pendingUsageLogCount == 0L) {
            println("No usage logs are pending. finish the job.")
            OTApp.logger.writeSystemLog("No usage logs are pending.", TAG)
            realm.close()
            return Result.success()
        }else{
            val serializedUsageLogs = realm.where(UsageLog::class.java).sort("timestamp").findAll()
                    .map { log ->
                        UsageLog.typeAdapter.toJson(log)
                    }
            OTApp.logger.writeSystemLog("Try uploading the usage logs. count: ${serializedUsageLogs.size}, average JSON length: ${serializedUsageLogs.asSequence().map { it.length }.average()} bytes", TAG)

            if(!isStopped) {
                val storedIds = usageLogUploadApi.get().uploadLocalUsageLogs(serializedUsageLogs).blockingGet()
                realm.executeTransaction {
                    realm.where(UsageLog::class.java).`in`("id", storedIds.toTypedArray())
                            .findAll().deleteAllFromRealm()
                }
            }
            realm.close()
            return if(isStopped) Result.success() else doWork()
        }
    }
}