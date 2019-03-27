package kr.ac.snu.hcil.omnitrack.core.triggers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.SystemClock
import androidx.core.app.AlarmManagerCompat
import androidx.work.*
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kr.ac.snu.hcil.android.common.ConcurrentUniqueLongGenerator
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.time.TimeHelper
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTTriggerMeasureEntry
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTDataDrivenTriggerCondition
import kr.ac.snu.hcil.omnitrack.receivers.DataDrivenTriggerCheckReceiver
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import org.jetbrains.anko.alarmManager
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class OTDataDrivenTriggerManager(private val context: Context, private val preferences: Lazy<SharedPreferences>, private val externalServiceManager: Lazy<OTExternalServiceManager>, private val realmFactory: Factory<Realm>) {

    companion object {
        const val TAG = "OTDataDrivenTriggerManager"
        const val WORK_NAME = "data-driven-condition-measure-check"

        const val DELAY_IMMEDIATE: Long = 1000
        const val DELAY_PERIODIC: Long = 5 * TimeHelper.minutesInMilli
        const val DELAY_RETRY: Long = 5000

        const val PREF_KEY_NEXT_CHECK_TIME_ELAPSED = "pref_data_driven_trigger_next_check_time_elapsed"


        const val REQUEST_CODE = 5244

        const val FIELD_FACTORY_CODE = "factoryCode"
        const val FIELD_SERIALIZED_MEASURE = "serializedMeasure"
        const val FIELD_MEASURE_HISTORY = "measureHistory"

        const val FIELD_TRIGGERS = "triggers"

        const val FIELD_MEASURED_VALUE = "measuredValue"
        const val FIELD_SERIALIZED_TIME_QUERY = "serializedTimeQuery"
        const val FIELD_IS_ACTIVE = "isActive"

        const val METADATA_KEY_TIMESTAMP = "measured_timestamp"
        const val METADATA_KEY_VALUE = "measured_value"
        const val METADATA_KEY_FACTORY_CODE = "measure_code"
    }

    private var suspendReadjustWorker = AtomicBoolean(false)

    @Inject
    lateinit var timeQueryTypeAdapter: Lazy<OTTimeRangeQuery.TimeRangeQueryTypeAdapter>
    private val measureEntryIdGenerator: ConcurrentUniqueLongGenerator by lazy { ConcurrentUniqueLongGenerator() }

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    fun setSuspendReadjustWorker(suspend: Boolean) {
        this.suspendReadjustWorker.set(suspend)
    }

    fun isAlarmRegistered(context: Context): Boolean = makePendingIntent(context, PendingIntent.FLAG_NO_CREATE) != null

    private fun matchCondition(condition: OTDataDrivenTriggerCondition, measureEntry: OTTriggerMeasureEntry): Boolean {
        val conditionMeasure = condition.measure
        if (conditionMeasure == null) {
            return false
        } else if (conditionMeasure.factoryCode == measureEntry.factoryCode) {
            if (condition.timeQuery.equals(timeQueryTypeAdapter.get().fromJson(measureEntry.serializedTimeQuery))) {
                val measureFactory = externalServiceManager.get().getMeasureFactoryByCode(conditionMeasure.factoryCode)
                if (measureFactory != null) {
                    return conditionMeasure.equals(measureFactory.makeMeasure(measureEntry.serializedMeasure!!))
                } else return false
            } else return false
        } else return false
    }

    private fun tryDisableMeasureEntryForTrigger(measureEntry: OTTriggerMeasureEntry, trigger: OTTriggerDAO, realm: Realm): Boolean {
        /*
        var removed = false
        realm.executeTransactionIfNotIn {
            measureEntry.triggers.remove(trigger)
            if (measureEntry.triggers.isEmpty()) {
                measureEntry.measureHistory.deleteAllFromRealm()
                measureEntry.deleteFromRealm()
                removed = true
            } else removed = false
        }

        return removed*/

        var disabled = false
        realm.executeTransactionIfNotIn {
            measureEntry.triggers.remove(trigger)
            if (measureEntry.triggers.isEmpty()) {
                measureEntry.isActive = false
                disabled = true
            } else disabled = false
        }
        return disabled

    }

    fun reAdjustWorker(realm: Realm) {
        if (!suspendReadjustWorker.get()) {
            OTApp.logger.writeSystemLog("readjust worker: currently alarm active in system: ${isAlarmRegistered(context)}", TAG)
            val numMeasures = realm.where(OTTriggerMeasureEntry::class.java)
                    .equalTo(FIELD_IS_ACTIVE, true).count()
            if (numMeasures > 0L) {
                //turn on worker
                OTApp.logger.writeSystemLog("${numMeasures} measures are active. register an alarm", TAG)
                reserveCheckExecution(context, DELAY_IMMEDIATE)
            } else {
                //turn off worker
                OTApp.logger.writeSystemLog("No measures are active. cancel the alarm", TAG)
                preferences.get().edit().remove(PREF_KEY_NEXT_CHECK_TIME_ELAPSED).apply()
                val pendingIntent = makePendingIntent(context)
                context.alarmManager.cancel(pendingIntent)
                pendingIntent?.cancel()
            }
        }
    }


    private fun makePendingIntent(context: Context, overrideFlag: Int? = null): PendingIntent? {
        val receiverIntent = DataDrivenTriggerCheckReceiver.makeIntent(context)
        return PendingIntent.getBroadcast(context, REQUEST_CODE, receiverIntent, overrideFlag ?: PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun activateOnSystem() {

        WorkManager.getInstance().enqueueUniquePeriodicWork(
                "CLEAR_INACTIVE_ENTRIES", ExistingPeriodicWorkPolicy.REPLACE,
                PeriodicWorkRequest.Builder(InactiveMeasureEntryClearanceWorker::class.java,
                        1, TimeUnit.DAYS
                )
                        .setConstraints(Constraints.Builder()
                                .apply {
                                    if (Build.VERSION.SDK_INT >= 23)
                                        setRequiresDeviceIdle(true)
                                }.setRequiresCharging(true).build())
                        .build()
        )

        realmFactory.get().use {
            reAdjustWorker(it)
        }
    }

    fun deleteInactiveMeasureEntries() {
        OTApp.logger.writeSystemLog("Remove inactive measure entries.", OTDataDrivenTriggerManager::class.java.simpleName)
        realmFactory.get().use { realm ->
            realm.executeTransactionIfNotIn {
                val entries = it.where(OTTriggerMeasureEntry::class.java)
                        .equalTo(FIELD_IS_ACTIVE, false)
                        .findAll()
                entries.forEach { entry ->
                    entry.measureHistory.deleteAllFromRealm()
                }
                entries.deleteAllFromRealm()
            }
        }
    }

    fun reserveCheckExecution(context: Context, delayMillis: Long) {
        val nextTime = SystemClock.elapsedRealtime() + delayMillis
        preferences.get().edit().putLong(PREF_KEY_NEXT_CHECK_TIME_ELAPSED, nextTime).apply()
        AlarmManagerCompat.setExactAndAllowWhileIdle(context.alarmManager, AlarmManager.ELAPSED_REALTIME_WAKEUP,
                nextTime,
                makePendingIntent(context)!!)
    }

    fun getNextCheckTimeElapsed(): Long? {
        return preferences.get().getLong(PREF_KEY_NEXT_CHECK_TIME_ELAPSED, -1).let {
            if (it == -1L) null else it
        }
    }

    private fun tryRegisterNewMeasure(trigger: OTTriggerDAO, realm: Realm) {
        val condition = trigger.condition as OTDataDrivenTriggerCondition
        val measure = condition.measure
        if (measure != null) {
            val factory = externalServiceManager.get().getMeasureFactoryByCode(measure.factoryCode)
            if (factory != null) {
                realm.executeTransactionIfNotIn {
                    val matchedMeasureEntry = it.where(OTTriggerMeasureEntry::class.java).equalTo(FIELD_FACTORY_CODE, measure.factoryCode).findAll().find {
                        matchCondition(condition, it)
                    }
                    if (matchedMeasureEntry != null) {
                        if (!matchedMeasureEntry.triggers.contains(trigger)) {
                            matchedMeasureEntry.triggers.add(trigger)
                            matchedMeasureEntry.isActive = true
                        }
                    } else {
                        //create new one.
                        val newMeasureEntry = realm.createObject(OTTriggerMeasureEntry::class.java, measureEntryIdGenerator.getNewUniqueLong())
                        newMeasureEntry.triggers.add(trigger)
                        newMeasureEntry.factoryCode = measure.factoryCode
                        newMeasureEntry.serializedMeasure = factory.serializeMeasure(measure)
                        newMeasureEntry.serializedTimeQuery = timeQueryTypeAdapter.get().toJson(condition.timeQuery)
                        newMeasureEntry.isActive = true
                    }
                }
            }
        }
    }

    fun registerTrigger(trigger: OTTriggerDAO): Boolean {
        var newRegistered = false
        val realm = realmFactory.get()
        realm.executeTransactionIfNotIn {
            if ((trigger.measureEntries?.count() ?: 0) > 0) {
                //there already exists a measure entry.
                val measureEntry = trigger.measureEntries?.first()!!
                if (matchCondition(trigger.condition as OTDataDrivenTriggerCondition, measureEntry)) {
                    //the trigger matches the current condition. Finish the logic.
                    if (measureEntry.isActive == false) {
                        measureEntry.isActive = true
                        reAdjustWorker(realm)
                    }
                    newRegistered = false
                    return@executeTransactionIfNotIn
                } else {
                    //the entry measure does not match the current one. remove the measure and register new one.
                    tryDisableMeasureEntryForTrigger(measureEntry, trigger, it)
                    tryRegisterNewMeasure(trigger, it)
                    newRegistered = true
                    reAdjustWorker(realm)
                    return@executeTransactionIfNotIn
                }
            } else {
                //there are no entries. Register new one.
                tryRegisterNewMeasure(trigger, it)
                newRegistered = true
                reAdjustWorker(realm)
                return@executeTransactionIfNotIn
            }
        }
        realm.close()
        return newRegistered
    }

    fun unregisterTrigger(trigger: OTTriggerDAO): Boolean {
        var numMeasuresRemoved = 0
        val realm = realmFactory.get()
        realm.executeTransactionIfNotIn {
            trigger.measureEntries?.forEach { measureEntry ->
                if (tryDisableMeasureEntryForTrigger(measureEntry, trigger, realm)) {
                    numMeasuresRemoved++
                }
            }
        }
        realm.close()

        return if (numMeasuresRemoved > 0) {
            reAdjustWorker(realm)
            true
        } else false
    }

    fun makeLatestMeasuredValueObservable(triggerId: String): Flowable<Nullable<Pair<Double?, Long>>> {
        return Flowable.defer {
            val realm = realmFactory.get()
            return@defer realm.where(OTTriggerDAO::class.java).equalTo(BackendDbManager.FIELD_OBJECT_ID, triggerId)
                    .findAllAsync()
                    .asFlowable()
                    .filter {
                        it.isLoaded && it.isValid && it.isNotEmpty()
                    }.map { triggers ->
                        val trigger = triggers.first()!!
                        val measure = trigger.measureEntries?.filter { it.isActive }?.firstOrNull()
                        if (measure != null) {
                            return@map Nullable(measure)
                        } else {
                            //find match
                            val condition = trigger.condition as OTDataDrivenTriggerCondition
                            return@map Nullable(realm.where(OTTriggerMeasureEntry::class.java)
                                    .equalTo(FIELD_FACTORY_CODE, condition.measure!!.factoryCode)
                                    .equalTo(FIELD_IS_ACTIVE, true)
                                    .findAll()
                                    .find {
                                        matchCondition(condition, it)
                                    })
                            /*.findAllAsync().asFlowable()
                            .filter {
                                it.isLoaded && it.isValid
                            }.map {
                                val matchedEntry = it.findLast { matchCondition(condition, it) }
                                return@map Nullable(matchedEntry)
                            }*/
                        }
                    }.flatMap { (measure) ->
                        if (measure != null) {
                            return@flatMap measure.asFlowable<OTTriggerMeasureEntry>().filter { it.isLoaded && it.isValid && it.triggers.any { it.objectId == triggerId } }
                                    .map { measure ->
                                        val latestHistoryEntry = measure.measureHistory.maxBy { it.timestamp }
                                        return@map if (latestHistoryEntry != null) {
                                            Nullable(Pair(latestHistoryEntry.measuredValue, latestHistoryEntry.timestamp))
                                        } else Nullable<Pair<Double?, Long>>(null)
                                    }

                        } else Flowable.just(Nullable<Pair<Double?, Long>>(null))
                    }.doAfterTerminate {
                        realm.close()
                    }
        }
    }

    class InactiveMeasureEntryClearanceWorker(private val context: Context, workerParams: WorkerParameters) : RxWorker(context, workerParams) {

        @Inject
        lateinit var dataDrivenTriggerManager: Lazy<OTDataDrivenTriggerManager>

        init {
            (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
        }

        override fun getBackgroundScheduler(): Scheduler {
            return Schedulers.io()
        }

        override fun createWork(): Single<Result> {
            return Single.defer {
                dataDrivenTriggerManager.get().deleteInactiveMeasureEntries()
                return@defer Single.just(Result.success())
            }
        }

    }
}