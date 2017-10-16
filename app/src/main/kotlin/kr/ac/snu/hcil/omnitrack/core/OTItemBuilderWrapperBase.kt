package kr.ac.snu.hcil.omnitrack.core

import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTPendingItemBuilderDAO
import kr.ac.snu.hcil.omnitrack.utils.ValueWithTimestamp
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * Created by Young-Ho Kim on 16. 7. 25
 */
class OTItemBuilderWrapperBase(val dao: OTPendingItemBuilderDAO, val realm: Realm) {

    enum class EAttributeValueState {
        Processing, GettingExternalValue, Idle
    }

    interface AttributeStateChangedListener {
        fun onAttributeStateChanged(attributeLocalId: String, state: EAttributeValueState)
    }

    val keys: Set<String> by lazy { dao.data.mapNotNull { it.attributeLocalId }.toSet() }

    fun getValueInformationOf(attributeLocalId: String): ValueWithTimestamp? {
        return this.dao.data.find { it.attributeLocalId == attributeLocalId }?.let {
            ValueWithTimestamp(
                    it.serializedValue?.let { TypeStringSerializationHelper.deserialize(it) },
                    it.timestamp
            )
        }
    }

    fun saveToItem(itemDao: OTItemDAO?, loggingSource: ItemLoggingSource?): OTItemDAO {
        val itemDaoToSave = itemDao ?: OTItemDAO()
        if (itemDao == null) {
            itemDaoToSave.deviceId = OTApplication.app.deviceId
            itemDaoToSave.loggingSource = loggingSource ?: ItemLoggingSource.Unspecified
            itemDaoToSave.trackerId = dao.tracker?.objectId
        } else {
            itemDaoToSave.updatedAt = System.currentTimeMillis()
        }
        itemDaoToSave.synchronizedAt = null

        dao.data.forEach { builderFieldEntry ->
            builderFieldEntry.attributeLocalId?.let {
                itemDaoToSave.setValueOf(it, builderFieldEntry.serializedValue)
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
                val realm = OTApplication.app.databaseManager.getRealmInstance()
                Observable.merge(attributes.mapIndexed { i, attr: OTAttributeDAO ->
                    val attrLocalId = attr.localId
                    val connection = attr.getParsedConnection()
                    if (connection != null && connection.isAvailableToRequestValue()) {
                        //Connection
                        connection.getRequestedValue(this).flatMap { data ->
                            if (data.datum == null) {
                                println("ValueConnection result was null. send fallback value")
                                return@flatMap attr.getFallbackValue(realm)
                            } else {
                                println("Received valueConnection result - ${data.datum}")
                                return@flatMap Observable.just(data.datum)
                            }
                        }.onErrorResumeNext { err -> err.printStackTrace(); attr.getFallbackValue(realm) }.map { value -> Pair(attrLocalId, ValueWithTimestamp(value, System.currentTimeMillis())) }.subscribeOn(Schedulers.io()).doOnSubscribe {

                            onAttributeStateChangedListener?.onAttributeStateChanged(attrLocalId, EAttributeValueState.GettingExternalValue)
                        }
                    } else {

                        println("No connection. use fallback value: ${attrLocalId}")
                        return@mapIndexed attr.getFallbackValue(realm).map { nullable ->
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
                        }.doOnError { err -> err.printStackTrace(); realm.close() }.doOnCompleted {
                    realm.close()
                    println("RX finished autocompleting builder=======================")
                }
            }
        }
    }
}