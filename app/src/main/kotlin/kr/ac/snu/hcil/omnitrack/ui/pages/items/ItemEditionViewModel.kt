package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.support.v7.util.DiffUtil
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTPendingItemBuilderDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.RealmViewModel
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable
import rx.Single
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import rx.subscriptions.CompositeSubscription

/**
 * Created by Young-Ho on 10/9/2017.
 */
class ItemEditionViewModel : RealmViewModel(), OTItemBuilderWrapperBase.AttributeStateChangedListener {

    enum class ItemMode {
        Edit, New
    }

    enum class BuilderCreationMode {
        NewBuilder, Restored
    }

    var trackerDao: OTTrackerDAO? = null
        private set

    private lateinit var itemBuilderDao: OTPendingItemBuilderDAO
    private lateinit var builderWrapper: OTItemBuilderWrapperBase

    val trackerNameObservable = BehaviorSubject.create<String>()

    val modeObservable = BehaviorSubject.create<ItemMode>(ItemMode.New)
    val builderCreationModeObservable = BehaviorSubject.create<BuilderCreationMode>()

    val attributeViewModelListObservable = BehaviorSubject.create<List<AttributeInputViewModel>>()

    val isBusyObservable = BehaviorSubject.create<Boolean>(false)

    var isValid: Boolean = true
        private set

    var isBusy: Boolean
        get() {
            return isBusyObservable.value
        }
        private set(value) {
            if (isBusyObservable.value != value) {
                isBusyObservable.onNext(value)
            }
        }

    var onInitialized = PublishSubject.create<Pair<ItemMode, BuilderCreationMode>>()

    private val currentAttributeViewModelList = ArrayList<AttributeInputViewModel>()

    var mode: ItemMode
        get() {
            return modeObservable.value
        }
        private set(value) {
            if (modeObservable.value != value) {
                modeObservable.onNext(value)
            }
        }

    fun init(trackerId: String, itemId: String?) {
        isValid = true
        trackerDao = OTApplication.app.databaseManager.getTrackerQueryWithId(trackerId, realm).findFirst()
        trackerNameObservable.onNext(trackerDao?.name)
        subscriptions.clear()

        if (itemId != null) {
            mode = ItemMode.Edit
            onInitialized.onNext(Pair(mode, BuilderCreationMode.NewBuilder))
        } else {

            val builderDaoResult = OTApplication.app.databaseManager.getItemBuilderQuery(trackerId, OTPendingItemBuilderDAO.HOLDER_TYPE_INPUT_FORM, realm).findFirst()

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

            val unManagedTrackerDao = realm.copyFromRealm(trackerDao)!!

            currentAttributeViewModelList.forEach { it.unregister() }
            currentAttributeViewModelList.clear()

            currentAttributeViewModelList.addAll(unManagedTrackerDao.attributes.map { AttributeInputViewModel(it) })
            attributeViewModelListObservable.onNext(currentAttributeViewModelList)

            this.builderWrapper = OTItemBuilderWrapperBase(this.itemBuilderDao)

            for (key in this.builderWrapper.keys) {
                val value = this.builderWrapper.getValueInformationOf(key)
                if (value != null) {
                    setValueOfAttribute(key, value)
                }
            }

            mode = ItemMode.New

            onInitialized.onNext(Pair(mode, builderCreationModeObservable.value))
        }
    }

    fun startAutoComplete() {
        subscriptions.add(
                this.builderWrapper.makeAutoCompleteObservable(this).subscribe({ (attrLocalId, valueWithTimestamp) ->
                    setValueOfAttribute(attrLocalId, valueWithTimestamp)
                }, {

                }, {
                    isBusy = false
                })
        )
    }

    fun isViewModelsDirty(): Boolean {
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

    private fun setValueOfAttribute(attributeLocalId: String, valueWithTimestamp: OTItemBuilderWrapperBase.ValueWithTimestamp) {
        itemBuilderDao.setValue(attributeLocalId, valueWithTimestamp, realm)
        val match = currentAttributeViewModelList.find { it.attributeLocalId == attributeLocalId }
        if (match != null) {
            match.value = valueWithTimestamp
        }
    }

    override fun onAttributeStateChanged(attributeLocalId: String, state: OTItemBuilderWrapperBase.EAttributeValueState) {
        val match = currentAttributeViewModelList.find { it.attributeLocalId == attributeLocalId }
        if (match != null) {
            match.state = state
        }
    }

    private fun refreshDaoValues() {
        realm.executeTransaction {
            currentAttributeViewModelList.forEach { attrViewModel ->
                val value = attrViewModel.value
                itemBuilderDao.setValue(attrViewModel.attributeLocalId, value, realm)
            }
        }
    }

    fun saveItemBuilder() {
        if (isValid) {
            println("save builder")
            refreshDaoValues()
            subscriptions.add(
                    isBusyObservable.filter { it == false }.subscribe {
                        realm.executeTransaction {
                            realm.copyToRealmOrUpdate(itemBuilderDao)
                        }
                    }
            )
        }
    }

    fun applyBuilderToItem(): Single<Nullable<String>> {
        if (isValid) {
            return isBusyObservable.filter { it == false }.first().toSingle().flatMap {
                refreshDaoValues()
                var itemId: String? = null

                if (mode == ItemMode.New) {
                    val item = builderWrapper.saveToItem(null, OTItem.LoggingSource.Manual)
                    return@flatMap OTApplication.app.databaseManager.saveItemImpl(item).map { Nullable(it.second) }
                } else {
                    //TODO edit mode
                    throw NotImplementedError("")
                }
            }
        } else return Single.just(Nullable<String>(null))
    }

    fun removeItemBuilder() {
        subscriptions.add(
                isBusyObservable.filter { it == false }.subscribe {
                    realm.executeTransaction {
                        realm.where(OTPendingItemBuilderDAO::class.java).equalTo("id", itemBuilderDao.id).findAll().deleteAllFromRealm()
                    }
                    println("removed item builder.")
                    isValid = false
                }
        )
    }

    class AttributeInputViewModel(val attributeDAO: OTAttributeDAO) {
        val attributeLocalId: String get() = attributeDAO.localId
        val columnNameObservable: Observable<String> = BehaviorSubject.create<String>("")
        val stateObservable: Observable<OTItemBuilderWrapperBase.EAttributeValueState> = BehaviorSubject.create<OTItemBuilderWrapperBase.EAttributeValueState>()

        val valueObservable = BehaviorSubject.create<Nullable<OTItemBuilderWrapperBase.ValueWithTimestamp>>(Nullable(null) as Nullable<OTItemBuilderWrapperBase.ValueWithTimestamp>)

        var value: OTItemBuilderWrapperBase.ValueWithTimestamp?
            get() = valueObservable.value?.datum
            set(value) {
                if (valueObservable.value?.datum != value) {
                    valueObservable.onNext(Nullable(value))
                }
            }

        var state: OTItemBuilderWrapperBase.EAttributeValueState
            get() = (stateObservable as BehaviorSubject).value
            internal set(value) {
                if ((stateObservable as BehaviorSubject).value != value) {
                    stateObservable.onNext(value)
                }
            }

        private val subscriptions = CompositeSubscription()

        init {
            (columnNameObservable as BehaviorSubject).onNext(attributeDAO.name)
        }

        fun unregister() {
            subscriptions.clear()
        }

    }

    class AttributeInputViewModelListDiffUtilCallback(val oldList: List<AttributeInputViewModel>, val newList: List<AttributeInputViewModel>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] === newList[newItemPosition] ||
                    oldList[oldItemPosition].attributeDAO.objectId == newList[newItemPosition].attributeDAO.objectId
        }

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return areItemsTheSame(oldItemPosition, newItemPosition)
        }


    }
}