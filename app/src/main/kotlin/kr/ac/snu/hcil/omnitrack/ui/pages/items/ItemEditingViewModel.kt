package kr.ac.snu.hcil.omnitrack.ui.pages.items

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.database.local.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.ValueWithTimestamp
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Single

/**
 * Created by Young-Ho on 10/15/2017.
 */
class ItemEditingViewModel : ItemEditionViewModelBase() {

    private lateinit var originalUnmanagedItemDao: OTItemDAO

    override fun onInit(trackerDao: OTTrackerDAO, itemId: String?): Pair<ItemMode, BuilderCreationMode?>? {
        if (itemId != null) {
            val itemDao = OTApplication.app.databaseManager.makeSingleItemQuery(itemId, realm).findFirst()
            if (itemDao != null) {
                originalUnmanagedItemDao = realm.copyFromRealm(itemDao)

                return Pair(ItemMode.Edit, null)
            } else return null
        } else return null
    }


    override fun startAutoComplete() {
        originalUnmanagedItemDao.fieldValueEntries.forEach {
            setValueOfAttribute(it.key, ValueWithTimestamp(it.value?.let { TypeStringSerializationHelper.deserialize(it) }, null))
        }
    }

    override fun isViewModelsDirty(): Boolean {
        for (viewModel in currentAttributeViewModelList) {
            val match = originalUnmanagedItemDao.fieldValueEntries.find { it.key == viewModel.attributeLocalId }
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

    override fun cacheEditingInfo() {
    }

    override fun applyEditingToDatabase(): Single<Nullable<String>> {
        return if (isValid) {
            return isBusyObservable.filter { it == false }.first().toSingle().flatMap {
                if (isViewModelsDirty()) {
                    return@flatMap Single.defer {
                        val originalItemFieldKeys = originalUnmanagedItemDao.fieldValueEntries.map { it.key }.toMutableList()
                        for (viewModel in currentAttributeViewModelList) {
                            if (originalUnmanagedItemDao.setValueOf(viewModel.attributeLocalId, viewModel.value?.value?.let { TypeStringSerializationHelper.serialize(it) })) {
                                //changed value
                                originalItemFieldKeys -= viewModel.attributeLocalId
                            }
                        }
                        OTApplication.app.databaseManager.saveItemObservable(originalUnmanagedItemDao, false, originalItemFieldKeys.toTypedArray(), realm).map { Nullable(it.second) }
                    }
                } else Single.just(Nullable<String>(originalUnmanagedItemDao.objectId))
            }
        } else return Single.just(Nullable<String>(null))
    }

    override fun clearHistory() {

    }
}