package kr.ac.snu.hcil.omnitrack.core.triggers

import android.content.Context
import dagger.Lazy
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTTriggerMeasureEntry
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTDataDrivenTriggerCondition
import kr.ac.snu.hcil.omnitrack.utils.ConcurrentUniqueLongGenerator
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn

class OTDataDrivenTriggerManager(private val context: Context, private val externalServiceManager: Lazy<OTExternalServiceManager>, private val realmFactory: Factory<Realm>) {

    companion object {
        const val FIELD_FACTORY_CODE = "factoryCode"
        const val FIELD_SERIALIZED_MEASURE = "serializedMeasure"
        const val FIELD_MEASURE_HISTORY = "measureHistory"

        const val FIELD_TRIGGERS = "triggers"

        const val FIELD_MEASURED_VALUE = "measuredValue"
        const val FIELD_SERIALIZED_TIME_QUERY = "serializedTimeQuery"
    }

    private val measureEntryIdGenerator = ConcurrentUniqueLongGenerator()

    private fun matchCondition(trigger: OTTriggerDAO, measureEntry: OTTriggerMeasureEntry): Boolean {
        return false
    }

    private fun tryRemoveMeasureEntryForTrigger(measureEntry: OTTriggerMeasureEntry, trigger: OTTriggerDAO, realm: Realm): Boolean {
        var removed = false
        realm.executeTransactionIfNotIn {
            measureEntry.triggers.remove(trigger)
            if (measureEntry.triggers.isEmpty()) {
                measureEntry.measureHistory.deleteAllFromRealm()
                measureEntry.deleteFromRealm()
                turnOffWorkerIfNoMeasureEntries(it)
                removed = true
            } else removed = false
        }

        return removed
    }

    private fun turnOffWorkerIfNoMeasureEntries(realm: Realm): Boolean {
        return realm.where(OTTriggerMeasureEntry::class.java).count() == 0L
    }

    private fun tryRegisterNewMeasure(trigger: OTTriggerDAO, realm: Realm) {
        val condition = trigger.condition as OTDataDrivenTriggerCondition
        val measure = condition.measure
        if (measure != null) {
            val factory = externalServiceManager.get().getMeasureFactoryByCode(measure.factoryCode)
            if (factory != null) {
                realm.executeTransactionIfNotIn {
                    val matchedMeasureEntry = it.where(OTTriggerMeasureEntry::class.java).equalTo(FIELD_FACTORY_CODE, measure.factoryCode).findAll().find {
                        val measureOfEntry = factory.makeMeasure(it.serializedMeasure!!)
                        measureOfEntry.equals(measure)
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
                        newMeasureEntry.serializedTimeQuery = OTApp.daoSerializationComponent.dataDrivenConditionTypeAdapter().timeRangeQueryTypeAdapter.get().toJson(condition.timeQuery)

                        //TODO turn on the worker
                    }
                }
            }
        }
    }

    fun registerTrigger(trigger: OTTriggerDAO): Boolean {
        var newRegistered = false
        val realm = realmFactory.get()
        realm.executeTransaction {
            if ((trigger.measureEntries?.count() ?: 0) > 0) {
                //there already exists a measure entry.
                val measureEntry = trigger.measureEntries?.first()!!
                if (matchCondition(trigger, measureEntry)) {
                    //the trigger matches the current condition. Finish the logic.
                    newRegistered = false
                    return@executeTransaction
                } else {
                    //the entry measure does not match the current one. remove the measure and register new one.
                    tryRemoveMeasureEntryForTrigger(measureEntry, trigger, it)
                    tryRegisterNewMeasure(trigger, it)
                    newRegistered = true
                    return@executeTransaction
                }
            } else {
                //there are no entries. Register new one.
                tryRegisterNewMeasure(trigger, it)
                newRegistered = true
                return@executeTransaction
            }
        }
        realm.close()
        return newRegistered
    }

    fun unregisterTrigger(trigger: OTTriggerDAO): Boolean {
        return false
    }
}