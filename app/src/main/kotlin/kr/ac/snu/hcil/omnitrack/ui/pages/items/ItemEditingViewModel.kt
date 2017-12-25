package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Application
import io.reactivex.Maybe
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.utils.AnyValueWithTimestamp
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by Young-Ho on 10/15/2017.
 */
class ItemEditingViewModel(app: Application) : ItemEditionViewModelBase(app) {

    private lateinit var originalUnmanagedItemDao: OTItemDAO

    override fun onInit(trackerDao: OTTrackerDAO, itemId: String?): Pair<ItemMode, BuilderCreationMode?>? {
        if (itemId != null) {
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

                return Pair(ItemMode.Edit, null)
            } else return null
        } else return null
    }


    override fun startAutoComplete() {
        originalUnmanagedItemDao.fieldValueEntries.forEach {
            setValueOfAttribute(it.key, AnyValueWithTimestamp(it.value?.let { TypeStringSerializationHelper.deserialize(it) }, null))
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

    override fun cacheEditingInfo(): Single<Boolean> {
        return Single.just(false)
    }

    override fun applyEditingToDatabase(): Maybe<String> {
        return if (isValid) {
            return isBusyObservable.filter { !it }.firstOrError().flatMapMaybe {
                if (isViewModelsDirty()) {
                    return@flatMapMaybe Maybe.defer {
                        val originalItemFieldKeys = originalUnmanagedItemDao.fieldValueEntries.map { it.key }.toMutableList()
                        for (viewModel in currentAttributeViewModelList) {
                            if (originalUnmanagedItemDao.setValueOf(viewModel.attributeLocalId, viewModel.value?.value?.let { TypeStringSerializationHelper.serialize(it) })) {
                                //changed value
                                originalItemFieldKeys -= viewModel.attributeLocalId
                            }
                        }
                        originalUnmanagedItemDao.synchronizedAt = null
                        Maybe.fromSingle(dbManager.get().saveItemObservable(originalUnmanagedItemDao, false, originalItemFieldKeys.toTypedArray(), realm).map { it.second })
                                .doAfterSuccess { syncItemToServer() }
                    }
                } else Maybe.just(originalUnmanagedItemDao.objectId)
            }
        } else return Maybe.just(null)
    }

    override fun clearHistory() {

    }
}