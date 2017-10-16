package kr.ac.snu.hcil.omnitrack.ui.pages.items

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.ItemLoggingSource
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.database.local.OTItemBuilderFieldValueEntry
import kr.ac.snu.hcil.omnitrack.core.database.local.OTPendingItemBuilderDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.ValueWithTimestamp
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Single

/**
 * Created by Young-Ho on 10/9/2017.
 */
class NewItemCreationViewModel : ItemEditionViewModelBase() {

    private lateinit var itemBuilderDao: OTPendingItemBuilderDAO
    private lateinit var builderWrapper: OTItemBuilderWrapperBase

    override fun onInit(trackerDao: OTTrackerDAO, itemId: String?): Pair<ItemMode, BuilderCreationMode?>? {
        val builderDaoResult = OTApplication.app.databaseManager.getItemBuilderQuery(trackerDao.objectId!!, OTPendingItemBuilderDAO.HOLDER_TYPE_INPUT_FORM, realm).findFirst()

        if (builderDaoResult != null) {
            //there is a pending itemBuilder.
            this.itemBuilderDao = realm.copyFromRealm(builderDaoResult)
            builderCreationModeObservable.onNext(BuilderCreationMode.Restored)

        } else {
            //no pending itemBuilder.
            realm.executeTransaction {
                val newBuilderDao = realm.createObject(OTPendingItemBuilderDAO::class.java, (realm.where(OTPendingItemBuilderDAO::class.java).max("id")?.toLong() ?: 0) + 1)
                newBuilderDao.tracker = trackerDao
                newBuilderDao.holderType = OTPendingItemBuilderDAO.HOLDER_TYPE_INPUT_FORM
                this.itemBuilderDao = realm.copyFromRealm(newBuilderDao)
            }
            builderCreationModeObservable.onNext(BuilderCreationMode.NewBuilder)
        }

        this.builderWrapper = OTItemBuilderWrapperBase(this.itemBuilderDao, realm)

        for (key in this.builderWrapper.keys) {
            val value = this.builderWrapper.getValueInformationOf(key)
            if (value != null) {
                setValueOfAttribute(key, value)
            }
        }

        return Pair(ItemMode.New, builderCreationModeObservable.value)
    }

    override fun setValueOfAttribute(attributeLocalId: String, valueWithTimestamp: ValueWithTimestamp) {
        itemBuilderDao.setValue(attributeLocalId, valueWithTimestamp, realm)
        super.setValueOfAttribute(attributeLocalId, valueWithTimestamp)
    }

    override fun startAutoComplete() {
        isBusy = true
        subscriptions.add(
                this.builderWrapper.makeAutoCompleteObservable(this).subscribe({ (attrLocalId, valueWithTimestamp) ->
                    setValueOfAttribute(attrLocalId, valueWithTimestamp)
                }, {

                }, {
                    isBusy = false
                })
        )
    }

    override fun isViewModelsDirty(): Boolean {
        for (viewModel in currentAttributeViewModelList) {
            val match = itemBuilderDao.data.find { it.attributeLocalId == viewModel.attributeLocalId }
            if (match != null) {
                val vmValue = viewModel.value
                if (vmValue == null) {
                    return true
                } else {
                    if ((vmValue.timestamp != match.timestamp) || (vmValue.value != match.serializedValue?.let { TypeStringSerializationHelper.deserialize(it) })) {
                        return true
                    }
                }
            } else return viewModel.value != null
        }

        return false
    }

    private fun refreshDaoValues() {
        realm.executeTransaction {
            currentAttributeViewModelList.forEach { attrViewModel ->
                val value = attrViewModel.value
                itemBuilderDao.setValue(attrViewModel.attributeLocalId, value, realm)
            }
        }
    }

    override fun cacheEditingInfo() {
        if (isValid) {
            println("save builder")
            refreshDaoValues()
            subscriptions.add(
                    isBusyObservable.filter { it == false }.subscribe {
                        var highestBuilderEntryId = realm.where(OTItemBuilderFieldValueEntry::class.java).max("id")?.toLong() ?: 0L
                        itemBuilderDao.data.forEach {
                            if (!it.isManaged && it.id == -1L) {
                                it.id = ++highestBuilderEntryId
                            }
                        }
                        realm.executeTransaction {
                            realm.copyToRealmOrUpdate(itemBuilderDao)
                        }
                    }
            )
        }
    }

    override fun applyEditingToDatabase(): Single<Nullable<String>> {
        if (isValid) {
            return isBusyObservable.filter { it == false }.first().toSingle().flatMap {
                refreshDaoValues()
                val item = builderWrapper.saveToItem(null, ItemLoggingSource.Manual)
                return@flatMap OTApplication.app.databaseManager.saveItemObservable(item, false, null, realm).map { Nullable(it.second) }
            }
        } else return Single.just(Nullable<String>(null))
    }


    override fun clearHistory() {
        subscriptions.add(
                isBusyObservable.filter { it == false }.subscribe {
                    realm.executeTransaction {
                        val daos = realm.where(OTPendingItemBuilderDAO::class.java).equalTo("id", itemBuilderDao.id).findAll()
                        daos.forEach {
                            it.data.deleteAllFromRealm()
                        }
                        daos.deleteAllFromRealm()
                    }
                    println("removed item builder.")
                    isValid = false
                }
        )
    }

}