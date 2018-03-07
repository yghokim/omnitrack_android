package kr.ac.snu.hcil.omnitrack.services

import com.firebase.jobdispatcher.JobParameters
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.UsageLog
import kr.ac.snu.hcil.omnitrack.core.di.configured.UsageLogger
import kr.ac.snu.hcil.omnitrack.core.net.IUsageLogUploadAPI
import kr.ac.snu.hcil.omnitrack.utils.ConfigurableJobService
import javax.inject.Inject


/**
 * Created by younghokim on 2017. 11. 28..
 */
class OTUsageLogUploadService : ConfigurableJobService() {

    inner class ConfiguredTask(configuredContext: ConfiguredContext) : IConfiguredTask {

        private val subscriptions = CompositeDisposable()

        @field:[Inject UsageLogger]
        lateinit var realmFactory: Factory<Realm>

        @Inject
        lateinit var usageLogUploadApi: Lazy<IUsageLogUploadAPI>

        init {
            configuredContext.configuredAppComponent.inject(this)
        }


        @Synchronized
        private fun getPendingUsageLogCount(): Long {
            realmFactory.get().use { realm ->
                return realm.where(UsageLog::class.java).equalTo("isSynchronized", false).count()
            }
        }

        @Synchronized
        private fun fetchPendingUsageLogsSerialized(): Single<List<String>> {
            return Single.defer {
                val realm = realmFactory.get()
                realm.use { realm ->
                    return@defer Single.just(realm.where(UsageLog::class.java).equalTo("isSynchronized", false).sort("timestamp").findAll()
                            .map { log ->
                                UsageLog.typeAdapter.toJson(log)
                            })
                }
            }.subscribeOn(Schedulers.io())
        }

        override fun dispose() {
            subscriptions.clear()
        }

        override fun onStartJob(job: JobParameters): Boolean {
            val count = getPendingUsageLogCount()
            if (count == 0L) {
                println("No usage logs are pending. Finish service immediately")
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
                                            .findAll().deleteAllFromRealm()
                                    /*.findAll()
                                    .forEach { l ->
                                        l.isSynchronized = true
                                    }*/
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

        override fun onStopJob(job: JobParameters): Boolean {
            subscriptions.clear()
            return getPendingUsageLogCount() > 0
        }
    }

    override fun extractConfigIdOfJob(job: JobParameters): String {
        return job.tag.split(";").last()
    }

    override fun makeNewTask(configuredContext: ConfiguredContext): ConfiguredTask {
        return ConfiguredTask(configuredContext)
    }

}