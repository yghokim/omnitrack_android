package kr.ac.snu.hcil.omnitrack.core.triggers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import dagger.Lazy
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTTriggerMeasureEntry
import kr.ac.snu.hcil.omnitrack.core.di.global.DataDrivenTrigger
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTDataDrivenTriggerCondition
import kr.ac.snu.hcil.omnitrack.utils.ConcurrentUniqueLongGenerator
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import javax.inject.Inject

class OTDataDrivenTriggerManager(private val context: Context, private val externalServiceManager: Lazy<OTExternalServiceManager>, private val realmFactory: Factory<Realm>) {

    companion object {
        const val WORK_NAME = "data-driven-condition-measure-check"

        const val FIELD_FACTORY_CODE = "factoryCode"
        const val FIELD_SERIALIZED_MEASURE = "serializedMeasure"
        const val FIELD_MEASURE_HISTORY = "measureHistory"

        const val FIELD_TRIGGERS = "triggers"

        const val FIELD_MEASURED_VALUE = "measuredValue"
        const val FIELD_SERIALIZED_TIME_QUERY = "serializedTimeQuery"
    }

    @Inject
    lateinit var timeQueryTypeAdapter: Lazy<OTTimeRangeQuery.TimeRangeQueryTypeAdapter>

    @field:[Inject DataDrivenTrigger]
    lateinit var workRequest: Lazy<PeriodicWorkRequest>

    private val measureEntryIdGenerator = ConcurrentUniqueLongGenerator()

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
            WorkManager.getInstance().enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, workRequest.get())
        } else {
            //turn off worker
            WorkManager.getInstance().cancelUniqueWork(WORK_NAME)
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