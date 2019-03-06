package kr.ac.snu.hcil.omnitrack.core.triggers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.AlarmManagerCompat
import dagger.Lazy
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTTriggerMeasureEntry
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTDataDrivenTriggerCondition
import kr.ac.snu.hcil.omnitrack.receivers.DataDrivenTriggerCheckReceiver
import kr.ac.snu.hcil.omnitrack.utils.ConcurrentUniqueLongGenerator
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper
import org.jetbrains.anko.alarmManager
import javax.inject.Inject

class OTDataDrivenTriggerManager(private val context: Context, private val externalServiceManager: Lazy<OTExternalServiceManager>, private val realmFactory: Factory<Realm>) {

    companion object {
        const val WORK_NAME = "data-driven-condition-measure-check"

        const val REQUEST_CODE = 5244

        const val FIELD_FACTORY_CODE = "factoryCode"
        const val FIELD_SERIALIZED_MEASURE = "serializedMeasure"
        const val FIELD_MEASURE_HISTORY = "measureHistory"

        const val FIELD_TRIGGERS = "triggers"

        const val FIELD_MEASURED_VALUE = "measuredValue"
        const val FIELD_SERIALIZED_TIME_QUERY = "serializedTimeQuery"
    }

    @Inject
    lateinit var timeQueryTypeAdapter: Lazy<OTTimeRangeQuery.TimeRangeQueryTypeAdapter>
    private val measureEntryIdGenerator: ConcurrentUniqueLongGenerator by lazy { ConcurrentUniqueLongGenerator() }

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    private fun matchCondition(condition: OTDataDrivenTriggerCondition, measureEntry: OTTriggerMeasureEntry): Boolean {

        val measure = condition.measure
        if (measure != null) {
            val measureFactory = externalServiceManager.get().getMeasureFactoryByCode(measure.factoryCode)
            if (measureFactory != null) {
                return measure.equals(measureFactory.makeMeasure(measureEntry.serializedMeasure!!)) && condition.timeQuery.equals(timeQueryTypeAdapter.get().fromJson(measureEntry.serializedTimeQuery))
            } else {
                return false
            }
        } else return false
    }

    private fun tryRemoveMeasureEntryForTrigger(measureEntry: OTTriggerMeasureEntry, trigger: OTTriggerDAO, realm: Realm): Boolean {
        var removed = false
        realm.executeTransactionIfNotIn {
            measureEntry.triggers.remove(trigger)
            if (measureEntry.triggers.isEmpty()) {
                measureEntry.measureHistory.deleteAllFromRealm()
                measureEntry.deleteFromRealm()
                removed = true
            } else removed = false
        }

        return removed
    }

    private fun reAdjustWorker(realm: Realm) {
        val numMeasures = realm.where(OTTriggerMeasureEntry::class.java).count()
        if (numMeasures > 0L) {
            //turn on worker
            /*
            val workInfos = WorkManager.getInstance().getWorkInfosForUniqueWork(WORK_NAME).get()
            if (workInfos.isEmpty() || (workInfos.none { it.state === WorkInfo.State.RUNNING || it.state === WorkInfo.State.ENQUEUED }))
                WorkManager.getInstance().enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, workRequestBuilder.get().setInitialDelay(2, TimeUnit.SECONDS).build())
                */
            reserveCheckExecution(context, true)
        } else {
            //turn off worker
            /*
            WorkManager.getInstance().cancelUniqueWork(WORK_NAME)
            */
            context.alarmManager.cancel(makePendingIntent(context))
        }
    }


    private fun makePendingIntent(context: Context): PendingIntent {
        val receiverIntent = DataDrivenTriggerCheckReceiver.makeIntent(context)
        return PendingIntent.getBroadcast(context, REQUEST_CODE, receiverIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun activateOnSystem() {
        realmFactory.get().use {
            reAdjustWorker(it)
        }

    }

    fun reserveCheckExecution(context: Context, immediate: Boolean) {
        AlarmManagerCompat.setExactAndAllowWhileIdle(context.alarmManager, AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + (if (immediate) 1000 else 5 * TimeHelper.minutesInMilli),
                makePendingIntent(context))
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
                        if (!matchedMeasureEntry.triggers.contains(trigger))
                            matchedMeasureEntry.triggers.add(trigger)
                    } else {
                        //create new one.
                        val newMeasureEntry = realm.createObject(OTTriggerMeasureEntry::class.java, measureEntryIdGenerator.getNewUniqueLong())
                        newMeasureEntry.triggers.add(trigger)
                        newMeasureEntry.factoryCode = measure.factoryCode
                        newMeasureEntry.serializedMeasure = factory.serializeMeasure(measure)
                        newMeasureEntry.serializedTimeQuery = timeQueryTypeAdapter.get().toJson(condition.timeQuery)
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
                    newRegistered = false
                    return@executeTransactionIfNotIn
                } else {
                    //the entry measure does not match the current one. remove the measure and register new one.
                    tryRemoveMeasureEntryForTrigger(measureEntry, trigger, it)
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
                if (tryRemoveMeasureEntryForTrigger(measureEntry, trigger, realm)) {
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
}