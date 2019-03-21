package kr.ac.snu.hcil.omnitrack.core

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kr.ac.snu.hcil.android.common.containers.AnyValueWithTimestamp
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTItemBuilderDAO
import kr.ac.snu.hcil.omnitrack.core.di.global.ForGeneric
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import javax.inject.Inject
import javax.inject.Provider

/**
 * Created by Young-Ho Kim on 16. 7. 25
 */
class OTItemBuilderWrapperBase(val dao: OTItemBuilderDAO, val context: Context) {

    enum class EAttributeValueState {
        Processing, GettingExternalValue, Idle
    }

    interface AttributeStateChangedListener {
        fun onAttributeStateChanged(attributeLocalId: String, state: EAttributeValueState)
    }

    @field:[Inject ForGeneric]
    lateinit var gson: Lazy<Gson>

    val keys: Set<String> by lazy { dao.data.asSequence().mapNotNull { it.attributeLocalId }.toSet() }

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    fun getValueInformationOf(attributeLocalId: String): AnyValueWithTimestamp? {
        return this.dao.data.find { it.attributeLocalId == attributeLocalId }?.let {
            AnyValueWithTimestamp(
                    it.serializedValue?.let { TypeStringSerializationHelper.deserialize(it) },
                    it.timestamp
            )
        }
    }

    @Synchronized
    inline fun modifyMetadata(realm: Realm, crossinline handler: (metadata: JsonObject) -> Unit) {
        realm.executeTransactionIfNotIn {
            val obj = gson.get().fromJson(dao.serializedMetadata, JsonObject::class.java)
            handler.invoke(obj)
            dao.serializedMetadata = obj.toString()
        }
    }

    fun saveToItem(itemDao: OTItemDAO?, loggingSource: ItemLoggingSource?): OTItemDAO {
        val itemDaoToSave = itemDao ?: OTItemDAO()
        if (itemDao == null) {
            val app = (context.applicationContext as OTAndroidApp)
            itemDaoToSave.deviceId = app.deviceId
            itemDaoToSave.loggingSource = loggingSource ?: ItemLoggingSource.Unspecified
            itemDaoToSave.trackerId = dao.tracker?.objectId
            itemDaoToSave.timezone = app.applicationComponent.getPreferredTimeZone().id
        } else {
            itemDaoToSave.userUpdatedAt = System.currentTimeMillis()
        }
        itemDaoToSave.synchronizedAt = null

        dao.data.forEach { builderFieldEntry ->
            builderFieldEntry.attributeLocalId?.let {
                itemDaoToSave.setValueOf(it, builderFieldEntry.serializedValue)
            }
        }

        itemDaoToSave.serializedMetadata = dao.serializedMetadata

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
                    val connection = attr.getParsedConnection(context)
                    if (connection != null && connection.isAvailableToRequestValue(attr)) {
                        //Connection
                        connection.getRequestedValue(this).flatMap { data ->
                            if (data.datum == null) {
                                println("ValueConnection result was null. send fallback value")
                                return@flatMap attr.getFallbackValue(context, realm)
                            } else {
                                println("Received valueConnection result - ${data.datum}")
                                return@flatMap Single.just(data)
                            }
                        }.onErrorResumeNext { err: Throwable ->
                            err.printStackTrace()
                            OTApp.logger.writeSystemLog(Log.getStackTraceString(err), "OTItemBuilderWrapperBase")
                            attr.getFallbackValue(context, realm)
                        }.map { nullable: Nullable<out Any> -> Pair(attrLocalId, AnyValueWithTimestamp(nullable)) }
                                .subscribeOn(Schedulers.io())
                                .doOnSubscribe {
                                    onAttributeStateChangedListener?.onAttributeStateChanged(attrLocalId, EAttributeValueState.GettingExternalValue)
                                }.toObservable()
                    } else {
                        println("No connection. use fallback value: $attrLocalId")
                        return@mapIndexed attr.getFallbackValue(context, realm).map { nullable ->
                            println("No connection. received fallback value: $attrLocalId, ${nullable.datum}")
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
                                dao.setValue(attrLocalId, value)
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