package kr.ac.snu.hcil.omnitrack.core

import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTPendingItemBuilderDAO
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*

/**
 * Created by Young-Ho Kim on 16. 7. 25
 */
class OTItemBuilderWrapperBase {

    companion object {
        const val MODE_EDIT = 2
        const val MODE_FOREGROUND = 1
        const val MODE_BACKGROUND = 0
    }

    enum class EAttributeValueState {
        Processing, GettingExternalValue, Idle
    }

    interface AttributeStateChangedListener {
        fun onAttributeStateChanged(attributeId: String, state: EAttributeValueState)
    }

    data class ValueWithTimestamp(var value: Any?, var timestamp: Long) {
        override fun toString(): String {
            return "TimestampValueInfo{ timestamp: ${timestamp}, value: ${value}, valueType: ${value?.javaClass}}"
        }
    }

    private val attributeStateTable = Hashtable<String, EAttributeValueState>()

    protected val valueTable = Hashtable<String, ValueWithTimestamp>()

    val isEmpty: Boolean get() = valueTable.isEmpty

    val keys: Set<String> get() = valueTable.keys

    private val dao: OTPendingItemBuilderDAO

    /**
     * Used when editing item.
     * @param item: item should be already stored in DB. (Every item is immediately stored in DB when created.)
     */
    constructor(dao: OTPendingItemBuilderDAO, realm: Realm) {
        if (dao.isManaged) {
            this.dao = realm.copyFromRealm(dao)
        } else this.dao = dao

        this.dao.data.forEach { it ->
            valueTable[it.attributeLocalId] = ValueWithTimestamp(it.serializedValue, it.timestamp)
        }
    }

    fun saveCurrentBuilder() {

    }

    fun getValueInformationOf(attributeLocalId: String): ValueWithTimestamp? {
        return valueTable[attributeLocalId]
    }

    fun getValueWithKey(key: String): Any? {
        return valueTable[key]?.value
    }

    fun setValueOf(attributeLocalId: String, value: Any?, timestamp: Long = System.currentTimeMillis()) {
        synchronized(valueTable) {
            val info = valueTable[attributeLocalId]
            if (info != null) {
                info.value = value
                info.timestamp = timestamp
            } else {
                valueTable[attributeLocalId] = ValueWithTimestamp(value, timestamp)
            }
        }
    }

    fun removeValueOf(attributeLocalId: String) {
        valueTable.remove(attributeLocalId)
    }

    fun hasValueOf(attributeLocalId: String): Boolean {
        return valueTable.containsKey(attributeLocalId)
    }

    fun getNumStoredAttributes(): Int {
        return valueTable.keys.size
    }

    fun clear() {
        valueTable.clear()
    }

    fun getAttributeValueState(attributeLocalId: String): EAttributeValueState? {
        return attributeStateTable[attributeLocalId]
    }

    fun autoComplete(onAttributeStateChangedListener: AttributeStateChangedListener? = null): Observable<Pair<String, Any?>> {

        return Observable.defer {
            val attributes = dao.tracker?.attributes
            if (attributes == null) {
                return@defer Observable.empty<Pair<String, Any?>>()
            } else {
                Observable.merge(attributes.mapIndexed { i, attr: OTAttributeDAO ->
                    val attrLocalId = attr.localId
                    val connection = attr.getParsedConnection()
                    if (connection != null) {
                        connection.getRequestedValue(this).flatMap { data ->
                            if (data.datum == null) {
                                attr.getFallbackValue()
                            } else {
                                Observable.just(data.datum)
                            }
                        }.onErrorResumeNext { attr.getFallbackValue() }.map { value -> Pair(attrLocalId, value) }.subscribeOn(Schedulers.io()).doOnSubscribe {

                            println("RX doOnSubscribe1: ${Thread.currentThread().name}")
                            attributeStateTable[attrLocalId] = EAttributeValueState.GettingExternalValue
                            onAttributeStateChangedListener?.onAttributeStateChanged(attrLocalId, EAttributeValueState.GettingExternalValue)
                        }
                    } else {
                        attr.getFallbackValue().map { nullable ->
                            Pair(attrLocalId, nullable.datum)
                        }.doOnSubscribe {
                            println("RX doOnSubscribe2: ${Thread.currentThread().name}")
                            attributeStateTable[attrLocalId] = EAttributeValueState.Processing
                            onAttributeStateChangedListener?.onAttributeStateChanged(attrLocalId, EAttributeValueState.Processing)
                        }
                    }
                }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).doOnSubscribe {
                    println("RX Subscribe to ITemBuilder Autocomplete: ${Thread.currentThread().name} ==========================================")
                }.doOnNext { result ->

                    println("RX doOnNext: ${Thread.currentThread().name}")
                    val attrLocalId = result.first
                    val value = result.second

                    attributeStateTable[attrLocalId] = EAttributeValueState.Idle

                    onAttributeStateChangedListener?.onAttributeStateChanged(attrLocalId, EAttributeValueState.Idle)
                    setValueOf(attrLocalId, value)
                }.doOnCompleted {
                    println("RX finished autocompleting builder=======================")
                    println("Value Table=======================")
                    for (entry in valueTable) {
                        println("attrLocalId: ${entry.key}   /    valueInfo: ${entry.value}")
                    }
                    println("===================================")
                }
            }
        }
    }
}