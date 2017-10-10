package kr.ac.snu.hcil.omnitrack.core

import kr.ac.snu.hcil.omnitrack.core.database.local.OTPendingItemBuilderDAO
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
        fun onAttributeStateChanged(attributeId: String, position: Int, state: EAttributeValueState)
    }

    data class ValueInfo(var value: Any?, var timestamp: Long)

    private val attributeStateList = ArrayList<EAttributeValueState>()

    protected val valueTable = Hashtable<String, ValueInfo>()

    val isEmpty: Boolean get() = valueTable.isEmpty

    val keys: Set<String> get() = valueTable.keys

    private val dao: OTPendingItemBuilderDAO

    /**
     * Used when editing item.
     * @param item: item should be already stored in DB. (Every item is immediately stored in DB when created.)
     */
    constructor(dao: OTPendingItemBuilderDAO) {
        this.dao = dao
        dao.data.forEach { it ->
            valueTable[it.attributeLocalId] = ValueInfo(it.serializedValue, it.timestamp)
        }
    }

    fun getValueInformationOf(attributeLocalId: String): ValueInfo? {
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
                valueTable[attributeLocalId] = ValueInfo(value, timestamp)
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

    fun getAttributeValueState(position: Int): EAttributeValueState {
        return attributeStateList[position]
    }
/*
    fun autoComplete(onAttributeStateChangedListener: AttributeStateChangedListener? = null): Observable<Pair<Int, Any?>> {

        return Observable.defer {
            val attributes = dao.tracker?.attributes
            if (attributes == null) {
                return@defer Observable.empty<Pair<Int, Any?>>()
            } else {
                Observable.merge(attributes.mapIndexed { i, attr ->
                    val connection = attr.getParsedConnection()
                    if (connection != null) {
                        connection.getRequestedValue(this).flatMap { data ->
                            if (data.datum == null) {
                                attr.getAutoCompleteValue()
                            } else {
                                Observable.just(data.datum)
                            }
                        }.onErrorResumeNext { attr.getAutoCompleteValue() }.map { value -> Pair(i, value) }.subscribeOn(Schedulers.io()).doOnSubscribe {

                            println("RX doOnSubscribe1: ${Thread.currentThread().name}")
                            attributeStateList[i] = EAttributeValueState.GettingExternalValue
                            onAttributeStateChangedListener?.onAttributeStateChanged(attr.localId.toString(), i, EAttributeValueState.GettingExternalValue)
                        }
                    } else {
                        attr.getAutoCompleteValue().map { data ->
                            Pair(i, data as Any)
                        }.doOnSubscribe {
                            println("RX doOnSubscribe2: ${Thread.currentThread().name}")
                            attributeStateList[i] = EAttributeValueState.Processing
                            onAttributeStateChangedListener?.onAttributeStateChanged(attr.localId.toString(), i, EAttributeValueState.Processing)
                        }
                    }
                }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).doOnSubscribe {
                    println("RX Subscribe to ITemBuilder Autocomplete: ${Thread.currentThread().name} ==========================================")
                }.doOnNext { result ->

                    println("RX doOnNext: ${Thread.currentThread().name}")
                    val index = result.first
                    val value = result.second

                    val attribute = tracker.attributes[index]
                    attributeStateList[index] = EAttributeValueState.Idle

                    println("attribute ${index} (${attribute.name}) was complete: ${value}")

                    onAttributeStateChangedListener?.onAttributeStateChanged(attribute, index, EAttributeValueState.Idle)
                    setValueOf(attribute, value)
                }.doOnCompleted {
                    println("RX finished autocompleting builder=======================")
                }
            }
        }
    }*/
}