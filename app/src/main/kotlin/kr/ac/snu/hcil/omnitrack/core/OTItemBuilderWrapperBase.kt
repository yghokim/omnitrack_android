package kr.ac.snu.hcil.omnitrack.core

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTPendingItemBuilderDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTStringStringEntryDAO
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*

/**
 * Created by Young-Ho Kim on 16. 7. 25
 */
class OTItemBuilderWrapperBase {

    enum class EAttributeValueState {
        Processing, GettingExternalValue, Idle
    }

    interface AttributeStateChangedListener {
        fun onAttributeStateChanged(attributeLocalId: String, state: EAttributeValueState)
    }

    data class ValueWithTimestamp(var value: Any?, var timestamp: Long) {
        override fun toString(): String {
            return "TimestampValueInfo{ timestamp: ${timestamp}, value: ${value}, valueType: ${value?.javaClass}}"
        }

        override fun equals(other: Any?): Boolean {
            return if (other === this) true else if (other is ValueWithTimestamp) {
                other.timestamp == timestamp && other.value == value
            } else false
        }
    }

    val keys: Set<String> by lazy { dao.data.mapNotNull { it.attributeLocalId }.toSet() }
    private val dao: OTPendingItemBuilderDAO

    constructor(dao: OTPendingItemBuilderDAO) {
        this.dao = dao
    }

    fun getValueInformationOf(attributeLocalId: String): ValueWithTimestamp? {
        return this.dao.data.find { it.attributeLocalId == attributeLocalId }?.let {
            ValueWithTimestamp(
                    it.serializedValue?.let { TypeStringSerializationHelper.deserialize(it) },
                    it.timestamp
            )
        }
    }

    fun saveToItem(itemDao: OTItemDAO?, loggingSource: OTItem.LoggingSource?): OTItemDAO {
        val itemDaoToSave = itemDao ?: OTItemDAO()
        if (itemDao == null) {
            itemDaoToSave.deviceId = OTApplication.app.deviceId
            itemDaoToSave.loggingSource = loggingSource ?: OTItem.LoggingSource.Unspecified
            itemDaoToSave.trackerId = dao.tracker?.objectId
        } else {
            itemDaoToSave.updatedAt = System.currentTimeMillis()
        }
        itemDaoToSave.synchronizedAt = null

        dao.data.forEach { builderFieldEntry ->
            val match = itemDaoToSave.fieldValueEntries.find { it.key == builderFieldEntry.attributeLocalId }
            if (match != null) {
                match.value = builderFieldEntry.serializedValue
            } else {
                itemDaoToSave.fieldValueEntries.add(
                        OTStringStringEntryDAO().apply {
                            id = UUID.randomUUID().toString()
                            key = builderFieldEntry.attributeLocalId!!
                            value = builderFieldEntry.serializedValue
                        }
                )
            }
        }

        return itemDaoToSave
    }

    fun makeAutoCompleteObservable(onAttributeStateChangedListener: AttributeStateChangedListener? = null): Observable<Pair<String, ValueWithTimestamp>> {

        return Observable.defer {
            val attributes = dao.tracker?.attributes
            if (attributes == null) {
                return@defer Observable.empty<Pair<String, ValueWithTimestamp>>()
            } else {
                Observable.merge(attributes.mapIndexed { i, attr: OTAttributeDAO ->
                    val attrLocalId = attr.localId
                    val connection = attr.getParsedConnection()
                    if (connection != null && connection.isAvailableToRequestValue()) {
                        //Connection
                        connection.getRequestedValue(this).flatMap { data ->
                            if (data.datum == null) {
                                println("ValueConnection result was null. send fallback value")
                                return@flatMap attr.getFallbackValue()
                            } else {
                                println("Received valueConnection result - ${data.datum}")
                                return@flatMap Observable.just(data.datum)
                            }
                        }.onErrorResumeNext { err -> err.printStackTrace(); attr.getFallbackValue() }.map { value -> Pair(attrLocalId, ValueWithTimestamp(value, System.currentTimeMillis())) }.subscribeOn(Schedulers.io()).doOnSubscribe {

                            onAttributeStateChangedListener?.onAttributeStateChanged(attrLocalId, EAttributeValueState.GettingExternalValue)
                        }
                    } else {

                        println("No connection. use fallback value: ${attrLocalId}")
                        return@mapIndexed attr.getFallbackValue().map { nullable ->
                            println("No connection. received fallback value: ${attrLocalId}, ${nullable.datum}")
                            Pair(attrLocalId, ValueWithTimestamp(nullable.datum, System.currentTimeMillis()))
                        }.doOnSubscribe {
                            onAttributeStateChangedListener?.onAttributeStateChanged(attrLocalId, EAttributeValueState.Processing)
                        }
                    }
                }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
                        .doOnNext { result ->

                            println("RX doOnNext: ${Thread.currentThread().name}")
                            val attrLocalId = result.first
                            val value = result.second

                            onAttributeStateChangedListener?.onAttributeStateChanged(attrLocalId, EAttributeValueState.Idle)
                        }.doOnCompleted {
                    println("RX finished autocompleting builder=======================")
                }
            }
        }
    }
}