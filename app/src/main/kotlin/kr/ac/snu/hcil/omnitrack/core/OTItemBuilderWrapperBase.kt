package kr.ac.snu.hcil.omnitrack.core

import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTPendingItemBuilderDAO
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

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

    constructor(dao: OTPendingItemBuilderDAO, realm: Realm) {
        if (dao.isManaged) {
            this.dao = realm.copyFromRealm(dao)
        } else this.dao = dao
    }

    fun getValueInformationOf(attributeLocalId: String): ValueWithTimestamp? {
        return this.dao.data.find { it.attributeLocalId == attributeLocalId }?.let {
            ValueWithTimestamp(
                    it.serializedValue?.let { TypeStringSerializationHelper.deserialize(it) },
                    it.timestamp
            )
        }
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
                    if (connection != null) {
                        connection.getRequestedValue(this).flatMap { data ->
                            if (data.datum == null) {
                                attr.getFallbackValue()
                            } else {
                                Observable.just(data.datum)
                            }
                        }.onErrorResumeNext { attr.getFallbackValue() }.map { value -> Pair(attrLocalId, ValueWithTimestamp(value, System.currentTimeMillis())) }.subscribeOn(Schedulers.io()).doOnSubscribe {

                            println("RX doOnSubscribe1: ${Thread.currentThread().name}")
                            onAttributeStateChangedListener?.onAttributeStateChanged(attrLocalId, EAttributeValueState.GettingExternalValue)
                        }
                    } else {
                        attr.getFallbackValue().map { nullable ->
                            Pair(attrLocalId, ValueWithTimestamp(nullable.datum, System.currentTimeMillis()))
                        }.doOnSubscribe {
                            println("RX doOnSubscribe2: ${Thread.currentThread().name}")
                            onAttributeStateChangedListener?.onAttributeStateChanged(attrLocalId, EAttributeValueState.Processing)
                        }
                    }
                }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).doOnSubscribe {
                    println("RX Subscribe to ITemBuilder Autocomplete: ${Thread.currentThread().name} ==========================================")
                }.doOnNext { result ->

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