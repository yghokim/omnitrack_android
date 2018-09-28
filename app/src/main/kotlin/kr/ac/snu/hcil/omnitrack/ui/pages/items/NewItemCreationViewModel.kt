package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Application
import android.os.Bundle
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.ItemLoggingSource
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.OTItemBuilderDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.OTItemBuilderFieldValueEntry
import kr.ac.snu.hcil.omnitrack.utils.AnyValueWithTimestamp
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import kr.ac.snu.hcil.omnitrack.utils.onNextIfDifferAndNotNull
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.util.*

/**
 * Created by Young-Ho on 10/9/2017.
 */
class NewItemCreationViewModel(app: Application) : ItemEditionViewModelBase(app) {

    companion object {
        const val KEY_RESERVED_NEW_ITEM_ID = "reservedNewItemId"
        const val KEY_REDIRECTED_PAGE_VISIT_STATUS = "redirectedPageVisitStatus"
        const val KEY_BUILDER_ID = "builderId"
    }

    enum class RedirectedPageStatus {
        RedirectionNotSet, NotVisited, Completed, Canceled
    }

    private lateinit var itemBuilderDao: OTItemBuilderDAO
    private lateinit var builderWrapper: OTItemBuilderWrapperBase

    val redirectedPageVisitStatusObservable = BehaviorSubject.createDefault<RedirectedPageStatus>(RedirectedPageStatus.RedirectionNotSet)


    private var reservedNewItemId: String? = null
    fun generateNewItemId(): String {
        this.reservedNewItemId = UUID.randomUUID().toString()
        return this.reservedNewItemId!!
    }

    override fun onInject(configuredContext: ConfiguredContext) {
        configuredContext.configuredAppComponent.inject(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_BUILDER_ID, this.itemBuilderDao.id)
        outState.putInt(KEY_REDIRECTED_PAGE_VISIT_STATUS, this.redirectedPageVisitStatusObservable.value?.ordinal
                ?: RedirectedPageStatus.RedirectionNotSet.ordinal)
        outState.putString(KEY_RESERVED_NEW_ITEM_ID, reservedNewItemId)
        val stored = cacheEditingInfo().blockingGet()
    }

    override fun onInit(trackerDao: OTTrackerDAO, itemId: String?, savedInstanceState: Bundle?) {

        if (!trackerDao.redirectUrl.isNullOrBlank()) {
            redirectedPageVisitStatusObservable.onNextIfDifferAndNotNull(RedirectedPageStatus.NotVisited)
        } else {
            redirectedPageVisitStatusObservable.onNextIfDifferAndNotNull(RedirectedPageStatus.RedirectionNotSet)
        }

        //val builderDaoResult = if (loadPendingItemBuilder) dbManager.get().getItemBuilderQuery(trackerDao.objectId!!, OTItemBuilderDAO.HOLDER_TYPE_INPUT_FORM, realm).findFirst() else null

        val storedBuilderDao = if (savedInstanceState != null && savedInstanceState.containsKey(KEY_BUILDER_ID)) {
            //there is a pending itemBuilder.
            realm.where(OTItemBuilderDAO::class.java).equalTo("id", savedInstanceState.getLong(KEY_BUILDER_ID)).findFirst()
        } else null

        if (storedBuilderDao == null) {
            val newBuilderId = System.currentTimeMillis()
            realm.executeTransactionIfNotIn {
                val newBuilderDao = realm.createObject(OTItemBuilderDAO::class.java, newBuilderId)
                newBuilderDao.tracker = trackerDao
                newBuilderDao.holderType = OTItemBuilderDAO.HOLDER_TYPE_INPUT_FORM
                this.itemBuilderDao = realm.copyFromRealm(newBuilderDao)
            }
        } else {
            this.itemBuilderDao = realm.copyFromRealm(storedBuilderDao)
        }

        this.builderWrapper = OTItemBuilderWrapperBase(this.itemBuilderDao, configuredContext)

        for (key in this.builderWrapper.keys) {
            val value = this.builderWrapper.getValueInformationOf(key)
            if (value != null) {
                setValueOfAttribute(key, value)
            }
        }

        if (savedInstanceState == null) {
            isBusy = true
            subscriptions.add(
                    this.builderWrapper.makeAutoCompleteObservable(realmProvider, this).subscribe({ (attrLocalId, valueWithTimestamp) ->
                        setValueOfAttribute(attrLocalId, valueWithTimestamp)
                    }, {

                    }, {
                        isBusy = false
                    })
            )
        } else {
            this.reservedNewItemId = savedInstanceState.getString(KEY_RESERVED_NEW_ITEM_ID)
            this.redirectedPageVisitStatusObservable.onNextIfDifferAndNotNull(RedirectedPageStatus.values()[savedInstanceState.getInt(KEY_REDIRECTED_PAGE_VISIT_STATUS)])
        }
    }

    override fun setValueOfAttribute(attributeLocalId: String, valueWithTimestamp: AnyValueWithTimestamp) {
        itemBuilderDao.setValue(attributeLocalId, valueWithTimestamp)
        super.setValueOfAttribute(attributeLocalId, valueWithTimestamp)
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
                itemBuilderDao.setValue(attrViewModel.attributeLocalId, value)
            }
        }
    }

    override fun cacheEditingInfo(): Single<Boolean> {
        return Single.defer {
            if (isValid) {
                println("save builder")
                refreshDaoValues()
                isBusyObservable.filter { !it }.firstOrError().map { isBusy ->
                    if (!isBusy) {
                        var highestBuilderEntryId = realm.where(OTItemBuilderFieldValueEntry::class.java).max("id")?.toLong()
                                ?: 0L
                        itemBuilderDao.data.forEach {
                            if (!it.isManaged && it.id == -1L) {
                                it.id = ++highestBuilderEntryId
                            }
                        }
                        realm.executeTransaction {
                            realm.insertOrUpdate(itemBuilderDao)
                        }
                        true
                    } else false
                }
            } else Single.just(false)
        }
    }

    override fun applyEditingToDatabase(): Maybe<String> {
        if (isValid) {
            return isBusyObservable.filter { !it }.firstOrError().flatMapMaybe {
                refreshDaoValues()
                println(metadataForItem)
                val item = builderWrapper.saveToItem(null, ItemLoggingSource.Manual, metadataForItem)
                item.objectId = reservedNewItemId

                return@flatMapMaybe Maybe.fromSingle<String>(dbManager.get().saveItemObservable(item, false, null, realm).map { it.second })
                        .doAfterSuccess {
                            reservedNewItemId = null
                            syncItemToServer()
                            OTApp.logger.writeSystemLog("Logged new item in the page. metadata: \n${item.serializedMetadata}", "NewItemCreationViewModel")
                        }
            }
        } else return Maybe.just(null)
    }


    override fun clearHistory() {
        subscriptions.add(
                isBusyObservable.filter { !it }.subscribe { isBusy ->
                    realm.executeTransaction { transactionRealm ->
                        val daos = transactionRealm.where(OTItemBuilderDAO::class.java).equalTo("id", itemBuilderDao.id).findAll()
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


    fun setResultOfRedirectedPage(result: RedirectedPageStatus) {
        this.redirectedPageVisitStatusObservable.onNextIfDifferAndNotNull(result)
    }

}