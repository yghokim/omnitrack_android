package kr.ac.snu.hcil.omnitrack.receivers

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.JsonObject
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kr.ac.snu.hcil.android.common.ConcurrentUniqueLongGenerator
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.system.WakefulBroadcastReceiverStaticLock
import kr.ac.snu.hcil.android.common.time.TimeHelper
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.connection.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTTriggerMeasureEntry
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTTriggerMeasureHistoryEntry
import kr.ac.snu.hcil.omnitrack.core.di.global.Backend
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import kr.ac.snu.hcil.omnitrack.core.externals.OTServiceMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.triggers.OTDataDrivenTriggerManager
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTDataDrivenTriggerCondition
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import org.jetbrains.anko.getStackTraceString
import javax.inject.Inject

class DataDrivenTriggerCheckReceiver : BroadcastReceiver() {

    companion object {
        val lockImpl = WakefulBroadcastReceiverStaticLock()

        const val ACTION_PERIODIC_CHECK = "${BuildConfig.APPLICATION_ID}.action.data_driven_trigger_check"

        fun makeIntent(context: Context): Intent {
            return Intent(context, DataDrivenTriggerCheckReceiver::class.java)
                    .setAction(ACTION_PERIODIC_CHECK)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, DataDrivenConditionHandlingService::class.java)
        lockImpl.startWakefulService(context, serviceIntent)
    }

    class DataDrivenConditionHandlingService : Service() {


        @field:[Inject Backend]
        lateinit var realmFactory: Factory<Realm>

        @Inject
        lateinit var externalServiceManager: Lazy<OTExternalServiceManager>

        @Inject
        lateinit var dataDrivenTriggerManager: Lazy<OTDataDrivenTriggerManager>

        @Inject
        lateinit var timeQueryTypeAdapter: Lazy<OTTimeRangeQuery.TimeRangeQueryTypeAdapter>

        private val historyEntryIdGenerator = ConcurrentUniqueLongGenerator()

        private val creationSubscriptions = CompositeDisposable()

        override fun onCreate() {
            super.onCreate()
            (application as OTAndroidApp).applicationComponent.inject(this)

            OTApp.logger.writeSystemLog("create data driven trigger check service.", "OTDataTriggerConditionWorker")
        }

        override fun onDestroy() {
            super.onDestroy()
            creationSubscriptions.clear()
            val nextCheckTime = dataDrivenTriggerManager.get().getNextCheckTimeElapsed()

            OTApp.logger.writeSystemLog("destroy data driven trigger check service. NextCheckTime: ${nextCheckTime}", "OTDataTriggerConditionWorker")
            if (nextCheckTime != null) {

            }
        }

        override fun onBind(intent: Intent?): IBinder? {
            return null
        }

        private fun fetchUnmanagedMeasures(): List<OTTriggerMeasureEntry>? {
            val realm = realmFactory.get()
            val entryQuery = realm
                    .where(OTTriggerMeasureEntry::class.java)
                    .equalTo(OTDataDrivenTriggerManager.FIELD_IS_ACTIVE, true)
            return if (entryQuery.count() > 0) {
                val list = realm.copyFromRealm(entryQuery.findAll(), 3)
                realm.close()
                list
            } else {
                realm.close()
                null
            }
        }

        private fun makeReminderDeliverable(measure: OTMeasureFactory.OTMeasure, measuredValue: Double, timestamp: Long): JsonObject {
            return jsonObject(
                    "measure_code" to measure.getFactory<OTServiceMeasureFactory>().typeCode,
                    "measured_value" to measuredValue,
                    "measured_timestamp" to timestamp)
        }

        override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
            this.creationSubscriptions.add(
                    Completable.defer {
                        val measureEntries = fetchUnmanagedMeasures()
                        if (measureEntries != null) {
                            val entryCommands = ArrayList<Single<Pair<OTTriggerMeasureEntry, Nullable<out Any>>>>()
                            val entriesGroupedByService = measureEntries.filter { it.factoryCode != null }
                                    .groupBy { externalServiceManager.get().getMeasureFactoryByCode(it.factoryCode!!)?.parentService }

                            entriesGroupedByService.forEach { (service, entries) ->
                                if (service != null && service.state == OTExternalService.ServiceState.ACTIVATED) {
                                    val valueRequests = entries.map { entry ->
                                        val factory = externalServiceManager.get().getMeasureFactoryByCode(entry.factoryCode!!)!!
                                        val measure = factory.makeMeasure(entry.serializedMeasure!!)
                                        measure.getValueRequest(null, timeQueryTypeAdapter.get().fromJson(entry.serializedTimeQuery)).map {
                                            Pair(entry, it)
                                        }.subscribeOn(Schedulers.io())
                                    }
                                    entryCommands.addAll(valueRequests)
                                } else {
                                    //store null value
                                    entryCommands.addAll(
                                            entries.map {
                                                Single.just(kotlin.Pair(it, Nullable(null) as Nullable<out Any>))
                                            }
                                    )
                                }
                            }

                            return@defer Single.merge(entryCommands)
                                    .subscribeOn(Schedulers.io())
                                    .flatMapCompletable { (entry, nullableValue) ->
                                        val realm = realmFactory.get()
                                        val measuredValue = nullableValue.datum?.toString()?.toDoubleOrNull()
                                        val now = System.currentTimeMillis()

                                        realm.executeTransactionIfNotIn {
                                            val newMeasuredValueEntry = it.createObject(OTTriggerMeasureHistoryEntry::class.java, historyEntryIdGenerator.getNewUniqueLong())
                                            newMeasuredValueEntry.timestamp = now
                                            newMeasuredValueEntry.measuredValue = measuredValue
                                            it.where(OTTriggerMeasureEntry::class.java).equalTo("id", entry.id).findFirst()?.measureHistory?.add(newMeasuredValueEntry)
                                            OTApp.logger.writeSystemLog("measure value of ${entry.factoryCode}: ${newMeasuredValueEntry.measuredValue}", "OTDataTriggerConditionWorker")
                                        }
                                        realm.close()

                                        val triggerFireJobs = ArrayList<Completable>()
                                        if (measuredValue != null) {
                                            val bigDecimalValue = measuredValue.toBigDecimal()
                                            entry.triggers.forEach { trigger ->
                                                val triggerCondition = trigger.condition as OTDataDrivenTriggerCondition
                                                if (triggerCondition.passesThreshold(bigDecimalValue)) {
                                                    val todayNonNullMeasures = entry.measureHistory.filter {
                                                        it.measuredValue != null && TimeHelper.isSameDay(it.timestamp, now) //TODO check timezone
                                                    }
                                                    if (todayNonNullMeasures.isEmpty()) {
                                                        //first history entry
                                                        triggerFireJobs.add(trigger.getPerformFireCompletable(now,
                                                                makeReminderDeliverable(triggerCondition.measure!!, measuredValue, now), this))
                                                    } else {
                                                        //check last non-null entry
                                                        val lastMeasureHistoryEntry = todayNonNullMeasures.last()
                                                        if (!triggerCondition.passesThreshold(lastMeasureHistoryEntry.measuredValue!!.toBigDecimal())) {
                                                            triggerFireJobs.add(trigger.getPerformFireCompletable(now,
                                                                    makeReminderDeliverable(triggerCondition.measure!!, measuredValue, now), this))
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        return@flatMapCompletable if (triggerFireJobs.isNotEmpty()) {
                                            Completable.merge(triggerFireJobs)
                                        } else Completable.complete()

                                    }
                        } else {
                            return@defer Completable.complete()
                        }
                    }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                            .doOnSubscribe {
                                OTApp.logger.writeSystemLog("start data trigger measure check", "OTDataTriggerConditionWorker")
                            }.doOnError {
                                OTApp.logger.writeSystemLog(it.getStackTraceString(), "OTDataTriggerConditionWorker")
                            }.subscribe({
                                OTApp.logger.writeSystemLog("finished data trigger measure check", "OTDataTriggerConditionWorker")
                                OTApp.logger.writeSystemLog("Register next data trigger measure check alarm.", "OTDataTriggerConditionWorker")
                                dataDrivenTriggerManager.get().reserveCheckExecution(this, OTDataDrivenTriggerManager.DELAY_PERIODIC)
                                lockImpl.completeWakefulIntent(intent)
                                stopSelf(startId)
                            }, { err ->
                                err.printStackTrace()
                                OTApp.logger.writeSystemLog("Retry measure check 5 seconds later", "OTDataTriggerConditionWorker")
                                dataDrivenTriggerManager.get().reserveCheckExecution(this, OTDataDrivenTriggerManager.DELAY_RETRY)
                                lockImpl.completeWakefulIntent(intent)
                                stopSelf(startId)
                            }))

            return START_STICKY
        }

    }
}