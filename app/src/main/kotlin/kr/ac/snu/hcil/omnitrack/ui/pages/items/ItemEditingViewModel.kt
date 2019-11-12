package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Application
import android.os.Bundle
import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.JsonArray
import io.reactivex.Maybe
import kr.ac.snu.hcil.android.common.containers.AnyValueWithTimestamp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.models.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.android.common.getLongCompat
import kr.ac.snu.hcil.android.common.getStringCompat

/**
 * Created by Young-Ho on 10/15/2017.
 */
class ItemEditingViewModel(app: Application) : ItemEditionViewModelBase(app) {

    private lateinit var originalUnmanagedItemDao: OTItemDAO

    fun init(trackerId: String, itemId: String, savedInstanceState: Bundle?) {
        if (init(trackerId)) {
            val itemDao = dbManager.get().makeSingleItemQuery(itemId, realm).findFirst()
            if (itemDao != null) {
                originalUnmanagedItemDao = realm.copyFromRealm(itemDao)

                subscriptions.add(
                        itemDao.asFlowable<OTItemDAO>().subscribe { dao ->
                            if (!dao.isValid) {
                                hasItemRemovedOutside.onNext(itemId)
                            }
                        }
                )

                if (savedInstanceState != null && savedInstanceState.containsKey("attributeValueArrayJson")) {
                    val valuesJson = savedInstanceState.getString("attributeValueArrayJson")!!
                    val valueArray = this.getApplication<OTApp>().applicationComponent.genericGson().fromJson(valuesJson, JsonArray::class.java)
                    valueArray.forEach { entry ->
                        val entryObj = entry.asJsonObject
                        setValueOfAttribute(
                                entryObj.getStringCompat("localId")!!,
                                AnyValueWithTimestamp(TypeStringSerializationHelper.deserialize(entryObj.getStringCompat("value")!!),
                                        entryObj.getLongCompat("timestamp")))
                    }
                } else {
                    originalUnmanagedItemDao.fieldValueEntries.forEach {
                        setValueOfAttribute(it.key, AnyValueWithTimestamp(it.value?.let { TypeStringSerializationHelper.deserialize(it) }, null))
                    }
                }
            } else throw IllegalArgumentException("No item with the id.")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val valueArray = JsonArray(currentAttributeViewModelList.size)
        currentAttributeViewModelList.forEach {
            val value = it.value
            if (value != null && value.value != null) {
                valueArray.add(
                        jsonObject(
                                "localId" to it.fieldLocalId,
                                "value" to TypeStringSerializationHelper.serialize(value.value!!),
                                "timestamp" to value.timestamp
                        )
                )
            }
        }
        outState.putString("attributeValueArrayJson", valueArray.toString())
    }

    override fun isViewModelsDirty(): Boolean {
        for (viewModel in currentAttributeViewModelList) {
            val match = originalUnmanagedItemDao.fieldValueEntries.find { it.key == viewModel.fieldLocalId }
            if (match != null) {
                val vmValue = viewModel.value
                if (vmValue != null && match.value != null) {
                    if (vmValue.value != match.value?.let { TypeStringSerializationHelper.deserialize(it) }) {
                        return true
                    }
                } else return true
            } else if (viewModel.value?.value != null) {
                return true
            }
        }
        return false
    }

    override fun applyEditingToDatabase(): Maybe<String> {
        if (isValid) {
            return isBusyObservable.filter { !it }.firstOrError().flatMapMaybe {
                if (isViewModelsDirty()) {
                    return@flatMapMaybe Maybe.defer {
                        val originalItemFieldKeys = originalUnmanagedItemDao.fieldValueEntries.asSequence().map { it.key }.toMutableList()
                        for (viewModel in currentAttributeViewModelList) {
                            if (originalUnmanagedItemDao.setValueOf(viewModel.fieldLocalId, viewModel.value?.value?.let { TypeStringSerializationHelper.serialize(it) })) {
                                //changed value
                                originalItemFieldKeys -= viewModel.fieldLocalId
                            }
                        }
                        originalUnmanagedItemDao.synchronizedAt = null
                        Maybe.fromSingle(dbManager.get().saveItemObservable(originalUnmanagedItemDao, false, originalItemFieldKeys.toTypedArray(), realm).map { it.second })
                                .doAfterSuccess { syncItemToServer() }
                    }
                } else Maybe.just(originalUnmanagedItemDao._id)
            }
        } else return Maybe.just(null)
    }
}