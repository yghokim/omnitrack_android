package kr.ac.snu.hcil.omnitrack.core.workers

import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTTriggerMeasureEntry
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTTriggerMeasureHistoryEntry
import kr.ac.snu.hcil.omnitrack.core.di.global.Backend
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import kr.ac.snu.hcil.omnitrack.utils.ConcurrentUniqueLongGenerator
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import javax.inject.Inject

class OTDataTriggerConditionWorker(val context: Context, workerParams: WorkerParameters) : RxWorker(context, workerParams) {

    @field:[Inject Backend]
    lateinit var realmFactory: Factory<Realm>

    @Inject
    lateinit var externalServiceManager: Lazy<OTExternalServiceManager>

    @Inject
    lateinit var timeQueryTypeAdapter: Lazy<OTTimeRangeQuery.TimeRangeQueryTypeAdapter>

    private val historyEntryIdGenerator = ConcurrentUniqueLongGenerator()

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }



    override fun createWork(): Single<Result> {
        return Single.defer {
            val realm = realmFactory.get()
            val entryQuery = realm.where(OTTriggerMeasureEntry::class.java)
            if (entryQuery.count() > 0) {
                val entryCommands = ArrayList<Flowable<Pair<OTTriggerMeasureEntry, Nullable<out Any>>>>()
                realm.executeTransactionIfNotIn {
                    val entriesGroupedByService = entryQuery.findAll().filter { it.factoryCode != null }
                            .groupBy { externalServiceManager.get().getMeasureFactoryByCode(it.factoryCode!!)?.parentService }

                    entriesGroupedByService.forEach { (service, entries) ->
                        if (service != null && service.state == OTExternalService.ServiceState.ACTIVATED) {
                            val valueRequests = entries.map { entry ->
                                val factory = externalServiceManager.get().getMeasureFactoryByCode(entry.factoryCode!!)!!
                                val measure = factory.makeMeasure(entry.serializedMeasure!!)
                                measure.getValueRequest(null, timeQueryTypeAdapter.get().fromJson(entry.serializedTimeQuery)).map {
                                    Pair<OTTriggerMeasureEntry, Nullable<out Any>>(entry, it)
                                }
                            }
                            entryCommands.addAll(valueRequests)
                        } else {
                            //store null value
                            entryCommands.addAll(
                                    entries.map {
                                        Flowable.just(Pair(it, Nullable(null) as Nullable<out Any>))
                                    }
                            )
                        }
                    }

                }
                return@defer Flowable.merge(entryCommands, 3).observeOn(Schedulers.io()).subscribeOn(backgroundScheduler).doOnNext { (entry, nullableValue) ->
                    realm.executeTransactionIfNotIn {
                        val newMeasuredValue = it.createObject(OTTriggerMeasureHistoryEntry::class.java, historyEntryIdGenerator.getNewUniqueLong())
                        newMeasuredValue.timestamp = System.currentTimeMillis()
                        newMeasuredValue.measuredValue = nullableValue.datum?.toString()?.toDoubleOrNull()
                        OTApp.logger.writeSystemLog("measure value of ${entry.factoryCode}: ${nullableValue.datum}", "OTDataTriggerConditionWorker")
                    }
                }.lastOrError().map { Result.success() }.doAfterTerminate { realm.close() }
            } else {
                realm.close()
                return@defer Single.just(Result.success())
            }
        }.subscribeOn(backgroundScheduler)
                .doOnSubscribe {
            OTApp.logger.writeSystemLog("start data trigger measure check", "OTDataTriggerConditionWorker")
        }
    }

}