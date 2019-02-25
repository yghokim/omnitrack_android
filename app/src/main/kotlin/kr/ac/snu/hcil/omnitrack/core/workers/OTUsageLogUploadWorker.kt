package kr.ac.snu.hcil.omnitrack.core.workers

import android.content.Context
import android.util.Log
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.UsageLog
import kr.ac.snu.hcil.omnitrack.core.di.global.UsageLogger
import kr.ac.snu.hcil.omnitrack.core.net.IUsageLogUploadAPI
import java.util.concurrent.Executors
import javax.inject.Inject


/**
 * Created by younghokim on 2017. 11. 28..
 */
class OTUsageLogUploadWorker(private val context: Context, workerParams: WorkerParameters) : RxWorker(context, workerParams) {

    companion object {
        const val TAG = "OTUsageLogUploadWorker"
    }


    @field:[Inject UsageLogger]
    lateinit var realmFactory: Factory<Realm>

    @Inject
    lateinit var usageLogUploadApi: Lazy<IUsageLogUploadAPI>

    private val realmScheduler = Schedulers.from(Executors.newSingleThreadExecutor())

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    override fun getBackgroundScheduler(): Scheduler {
        return realmScheduler
    }

    override fun createWork(): Single<Result> {
        var realm: Realm
        return Single.defer {
            realm = realmFactory.get()
            val pendingUsageLogCount = realm.where(UsageLog::class.java).equalTo("isSynchronized", false).count()
            if (pendingUsageLogCount == 0L) {
                println("No usage logs are pending. finish the job.")
                OTApp.logger.writeSystemLog("No usage logs are pending.", TAG)
                Single.just(Result.success()).observeOn(realmScheduler).doFinally { realm.close() }
            } else {
                return@defer Single.defer {
                    val serializedUsageLogs = realm.where(UsageLog::class.java).sort("timestamp").findAll()
                            .map { log ->
                                UsageLog.typeAdapter.toJson(log)
                            }
                    OTApp.logger.writeSystemLog("Try uploading the usage logs. count: ${serializedUsageLogs.size}, average JSON length: ${serializedUsageLogs.asSequence().map { it.length }.average()} bytes", TAG)

                    usageLogUploadApi.get().uploadLocalUsageLogs(serializedUsageLogs)
                            .observeOn(realmScheduler)
                            .doOnSuccess { storedIds ->
                                realm.executeTransaction {
                                    realm.where(UsageLog::class.java).`in`("id", storedIds.toTypedArray())
                                            .findAll().deleteAllFromRealm()
                                }
                            }.flatMap { createWork() }.onErrorReturn { error ->
                                error.printStackTrace()
                                println("every logs were not uploaded well. retry next time.")
                                OTApp.logger.writeSystemLog("Failed to upload the usage logs.\n${Log.getStackTraceString(error)}", TAG)
                                Result.retry()
                            }
                }.observeOn(realmScheduler).doFinally { realm.close() }
            }
        }
    }

}