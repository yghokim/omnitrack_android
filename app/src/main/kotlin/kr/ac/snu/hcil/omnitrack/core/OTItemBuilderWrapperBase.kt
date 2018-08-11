package kr.ac.snu.hcil.omnitrack.core

import android.util.Log
import com.google.gson.JsonObject
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.OTItemBuilderDAO
import kr.ac.snu.hcil.omnitrack.utils.AnyValueWithTimestamp
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import javax.inject.Provider

/**
 * Created by Young-Ho Kim on 16. 7. 25
 */
class OTItemBuilderWrapperBase(val dao: OTItemBuilderDAO, val configuredContext: ConfiguredContext, val realm: Realm) {

    enum class EAttributeValueState {
        Processing, GettingExternalValue, Idle
    }

    interface AttributeStateChangedListener {
        fun onAttributeStateChanged(attributeLocalId: String, state: EAttributeValueState)
    }

    val keys: Set<String> by lazy { dao.data.mapNotNull { it.attributeLocalId }.toSet() }

    fun getValueInformationOf(attributeLocalId: String): AnyValueWithTimestamp? {
        return this.dao.data.find { it.attributeLocalId == attributeLocalId }?.let {
            AnyValueWithTimestamp(
                    it.serializedValue?.let { TypeStringSerializationHelper.deserialize(it) },
                    it.timestamp
            )
        }
    }

    fun saveToItem(itemDao: OTItemDAO?, loggingSource: ItemLoggingSource?, metadata: JsonObject?): OTItemDAO {
        val itemDaoToSave = itemDao ?: OTItemDAO()
        if (itemDao == null) {
            itemDaoToSave.deviceId = (configuredContext.applicationComponent.application()).deviceId
            itemDaoToSave.loggingSource = loggingSource ?: ItemLoggingSource.Unspecified
            itemDaoToSave.trackerId = dao.tracker?.objectId
            itemDaoToSave.timezone = configuredContext.configuredAppComponent.getPreferredTimeZone().id
            println("item timezone: ${itemDaoToSave.timezone}")
        } else {
            itemDaoToSave.userUpdatedAt = System.currentTimeMillis()
        }
        itemDaoToSave.synchronizedAt = null

        dao.data.forEach { builderFieldEntry ->
            builderFieldEntry.attributeLocalId?.let {
                itemDaoToSave.setValueOf(it, builderFieldEntry.serializedValue)
            }
        }

        itemDaoToSave.serializedMetadata = metadata?.toString()

        return itemDaoToSave
    }

    fun makeAutoCompleteObservable(realmProvider: Provider<Realm>, onAttributeStateChangedListener: AttributeStateChangedListener? = null, applyToBuilder: Boolean = false): Observable<Pair<String, AnyValueWithTimestamp>> {

        return Observable.defer {
            val attributes = dao.tracker?.attributes?.filter { !it.isHidden && !it.isInTrashcan }
            if (attributes == null) {
                return@defer Observable.empty<Pair<String, AnyValueWithTimestamp>>()
            } else {
                val realm = realmProvider.get()
                Observable.merge(attributes.mapIndexed { i, attr: OTAttributeDAO ->
                    val attrLocalId = attr.localId
                    val connection = attr.getParsedConnection(configuredContext)
                    if (connection != null && connection.isAvailableToRequestValue()) {
                        //Connection
                        connection.getRequestedValue(this).flatMap { data ->
                            if (data.datum == null) {
                                println("ValueConnection result was null. send fallback value")
                                return@flatMap attr.getFallbackValue(configuredContext, realm).toFlowable()
                            } else {
                                println("Received valueConnection result - ${data.datum}")
                                return@flatMap Flowable.just(data)
                            }
                        }.onErrorResumeNext { err: Throwable ->
                            err.printStackTrace()
                            OTApp.logger.writeSystemLog(Log.getStackTraceString(err), "OTItemBuilderWrapperBase")
                            attr.getFallbackValue(configuredContext, realm).toFlowable()
                        }.map { nullable: Nullable<out Any> -> Pair(attrLocalId, AnyValueWithTimestamp(nullable)) }
                                .subscribeOn(Schedulers.io())
                                .doOnSubscribe {
                                    onAttributeStateChangedListener?.onAttributeStateChanged(attrLocalId, EAttributeValueState.GettingExternalValue)
                                }.toObservable()
                    } else {

                        println("No connection. use fallback value: ${attrLocalId}")
                        return@mapIndexed attr.getFallbackValue(configuredContext, realm).map { nullable ->
                            println("No connection. received fallback value: ${attrLocalId}, ${nullable.datum}")
                            Pair(attrLocalId, AnyValueWithTimestamp(nullable))
                        }.doOnSubscribe {
                            onAttributeStateChangedListener?.onAttributeStateChanged(attrLocalId, EAttributeValueState.Processing)
                        }.toObservable()
                    }
                }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
                        .doOnNext { result ->

                            println("RX doOnNext: ${Thread.currentThread().name}")
                            val attrLocalId = result.first
                            val value = result.second
                            if (applyToBuilder) {
                                dao.setValue(attrLocalId, value, realm)
                            }

                            onAttributeStateChangedListener?.onAttributeStateChanged(attrLocalId, EAttributeValueState.Idle)
                        }.doOnError { err -> err.printStackTrace(); realm.close() }.doOnComplete {
                            realm.close()
                            println("RX finished autocompleting builder=======================")
                        }
            }
        }
    }
}