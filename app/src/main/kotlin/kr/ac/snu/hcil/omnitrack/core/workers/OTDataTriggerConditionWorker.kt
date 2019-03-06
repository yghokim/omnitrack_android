package kr.ac.snu.hcil.omnitrack.core.workers

import android.content.Context
import androidx.work.*
import com.google.gson.JsonObject
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.Completable
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
import kr.ac.snu.hcil.omnitrack.core.di.global.DataDrivenTrigger
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTDataDrivenTriggerManager.Companion.WORK_NAME
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTDataDrivenTriggerCondition
import kr.ac.snu.hcil.omnitrack.utils.ConcurrentUniqueLongGenerator
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper
import org.jetbrains.anko.getStackTraceString
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class OTDataTriggerConditionWorker(val context: Context, workerParams: WorkerParameters) : RxWorker(context, workerParams) {

    @field:[Inject Backend]
    lateinit var realmFactory: Factory<Realm>

    @Inject
    lateinit var externalServiceManager: Lazy<OTExternalServiceManager>

    @Inject
    lateinit var timeQueryTypeAdapter: Lazy<OTTimeRangeQuery.TimeRangeQueryTypeAdapter>

    @field:[Inject DataDrivenTrigger]
    lateinit var workRequestBuilder: Lazy<OneTimeWorkRequest.Builder>

    private val historyEntryIdGenerator = ConcurrentUniqueLongGenerator()

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    private fun fetchUnmanagedMeasures(): List<OTTriggerMeasureEntry>? {
        val realm = realmFactory.get()
        val entryQuery = realm.where(OTTriggerMeasureEntry::class.java)
        return if (entryQuery.count() > 0) {
            val list = realm.copyFromRealm(entryQuery.findAll(), 3)
            realm.close()
            list
        } else {
            realm.close()
            null
        }
    }

    override fun createWork(): Single<Result> {
        return Single.defer {
            val measureEntries = fetchUnmanagedMeasures()
            if (measureEntries != null) {
                val entryCommands = ArrayList<Flowable<Pair<OTTriggerMeasureEntry, Nullable<out Any>>>>()
                val entriesGroupedByService = measureEntries.filter { it.factoryCode != null }
                        .groupBy { externalServiceManager.get().getMeasureFactoryByCode(it.factoryCode!!)?.parentService }

                entriesGroupedByService.forEach { (service, entries) ->
                    if (service != null && service.state == OTExternalService.ServiceState.ACTIVATED) {
                        val valueRequests = entries.map { entry ->
                            val factory = externalServiceManager.get().getMeasureFactoryByCode(entry.factoryCode!!)!!
                            val measure = factory.makeMeasure(entry.serializedMeasure!!)
                            measure.getValueRequest(null, timeQueryTypeAdapter.get().fromJson(entry.serializedTimeQuery)).map {
                                Pair(entry, it)
                            }
                        }
                        entryCommands.addAll(valueRequests)
                    } else {
                        //store null value
                        entryCommands.addAll(
                                entries.map {
                                    Flowable.just(kotlin.Pair(it, Nullable(null) as Nullable<out kotlin.Any>))
                                }
                        )
                    }
                }

                return@defer Flowable.merge(entryCommands, 3)
                        .observeOn(Schedulers.io())
                        .flatMapCompletable { (entry, nullableValue) ->
                            val realm = realmFactory.get()
                            val measuredValue = nullableValue.datum?.toString()?.toDoubleOrNull()
                            val now = System.currentTimeMillis()

                            realm.executeTransactionIfNotIn {
                                val newMeasuredValueEntry = it.createObject(OTTriggerMeasureHistoryEntry::class.java, historyEntryIdGenerator.getNewUniqueLong())
                                newMeasuredValueEntry.timestamp = now
                                newMeasuredValueEntry.measuredValue = measuredValue
                                entry.measureHistory.add(newMeasuredValueEntry)
                                it.insertOrUpdate(entry)
                                OTApp.logger.writeSystemLog("measure value of ${entry.factoryCode}: ${nullableValue.datum}", "OTDataTriggerConditionWorker")
                            }
                            realm.close()

                            val triggerFireJobs = ArrayList<Completable>()
                            if (measuredValue != null) {
                                val bigDecimalValue = measuredValue.toBigDecimal()
                                entry.triggers.forEach { trigger ->
                                    val triggerCondition = trigger.condition as OTDataDrivenTriggerCondition
                                    if (triggerCondition.passesThreshold(bigDecimalValue)) {
                                        if (entry.measureHistory.count() < 2) {
                                            //first history entry
                                            triggerFireJobs.add(trigger.getPerformFireCompletable(now, JsonObject(), context))
                                        } else {
                                            //check last entry
                                            val lastMeasureHistoryEntry = entry.measureHistory[entry.measureHistory.count() - 2]
                                            //TODO Check TimeZone
                                            if (TimeHelper.isSameDay(lastMeasureHistoryEntry!!.timestamp, now)) {
                                                if (lastMeasureHistoryEntry.measuredValue == null ||
                                                        !triggerCondition.passesThreshold(lastMeasureHistoryEntry.measuredValue!!.toBigDecimal())) {
                                                    triggerFireJobs.add(trigger.getPerformFireCompletable(now, JsonObject(), context))
                                                }
                                            } else {
                                                //this is the first log of the day.
                                                triggerFireJobs.add(trigger.getPerformFireCompletable(now, JsonObject(), context))
                                            }
                                        }
                                    }
                                }
                            }

                            return@flatMapCompletable if (triggerFireJobs.isNotEmpty()) {
                                Completable.merge(triggerFireJobs)
                            } else Completable.complete()

                        }.toSingle { Result.success() }
            } else {
                return@defer Single.just(Result.success())
            }
        }.doOnSuccess {
            if (it is Result.Success) {
                WorkManager.getInstance().enqueueUniqueWork(
                        WORK_NAME,
                        ExistingWorkPolicy.KEEP,
                        workRequestBuilder.get().setInitialDelay(3, TimeUnit.MINUTES).build())
            }
            OTApp.logger.writeSystemLog("finished data trigger measure check", "OTDataTriggerConditionWorker")
        }.doOnSubscribe {
            OTApp.logger.writeSystemLog("start data trigger measure check", "OTDataTriggerConditionWorker")
        }.doOnError {
            OTApp.logger.writeSystemLog(it.getStackTraceString(), "OTDataTriggerConditionWorker")
        }
    }

}