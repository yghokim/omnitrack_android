package kr.ac.snu.hcil.omnitrack.services

import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.models.helpermodels.UsageLog
import kr.ac.snu.hcil.omnitrack.core.di.UsageLogger
import kr.ac.snu.hcil.omnitrack.core.net.IUsageLogUploadAPI
import javax.inject.Inject


/**
 * Created by younghokim on 2017. 11. 28..
 */
class OTUsageLogUploadService : JobService() {
    companion object {
        const val TAG = "OTUsageLogUploadService"
    }

    @field:[Inject UsageLogger]
    lateinit var realmFactory: Factory<Realm>

    @Inject
    lateinit var usageLogUploadApi: Lazy<IUsageLogUploadAPI>


    private lateinit var realm: Realm

    private val subscriptions = CompositeDisposable()

    override fun onCreate() {
        super.onCreate()
        (application as OTApp).applicationComponent.inject(this)
        realm = realmFactory.get()
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
        realm.close()
    }

    @Synchronized
    private fun getPendingUsageLogCount(): Long {
        return realm.where(UsageLog::class.java).equalTo("isSynchronized", false).count()
    }

    @Synchronized
    fun fetchPendingUsageLogsSerialized(): Single<List<String>> {
        return Single.defer {
            val realm = realmFactory.get()
            realm.use { realm ->
                return@defer Single.just(realm.where(UsageLog::class.java).equalTo("isSynchronized", false).findAllSorted("timestamp")
                        .map { log ->
                            UsageLog.typeAdapter.toJson(log)
                        })
            }
        }.subscribeOn(Schedulers.io())
    }

    override fun onStopJob(job: JobParameters): Boolean {
        return getPendingUsageLogCount() > 0
    }

    override fun onStartJob(job: JobParameters): Boolean {
        println("try start upload usage logs...")
        val count = getPendingUsageLogCount()
        if (count == 0L) {
            return false
        } else {

            subscriptions.add(
                    fetchPendingUsageLogsSerialized().flatMap { list ->
                        return@flatMap usageLogUploadApi.get().uploadLocalUsageLogs(list)
                    }.doOnSuccess { storedIds ->
                        val realm = realmFactory.get()
                        realm.use {
                            realm.executeTransaction {
                                realm.where(UsageLog::class.java).`in`("id", storedIds.toTypedArray())
                                        .findAll()
                                        .forEach { l ->
                                            l.isSynchronized = true
                                        }
                            }
                        }
                    }.observeOn(AndroidSchedulers.mainThread()).subscribe({ list ->
                        if (!onStartJob(job)) {
                            println("every logs were uploaded. finish the job.")
                            jobFinished(job, false)
                        }
                    }, { error ->
                        error.printStackTrace()
                        println("every logs were not uploaded well. retry next time.")
                        jobFinished(job, true)
                    })
            )

            return true
        }

    }

}