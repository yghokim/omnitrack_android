package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.support.v7.util.DiffUtil
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.RealmViewModel
import kr.ac.snu.hcil.omnitrack.utils.ValueWithTimestamp

/**
 * Created by Young-Ho on 10/15/2017.
 */
abstract class ItemEditionViewModelBase : RealmViewModel(), OTItemBuilderWrapperBase.AttributeStateChangedListener {
    enum class ItemMode {
        Edit, New
    }

    enum class BuilderCreationMode {
        NewBuilder, Restored
    }

    var trackerDao: OTTrackerDAO? = null
        protected set

    val trackerNameObservable = BehaviorSubject.create<String>()
    val modeObservable = BehaviorSubject.createDefault<ItemMode>(ItemMode.New)
    val builderCreationModeObservable = BehaviorSubject.create<BuilderCreationMode>()
    val attributeViewModelListObservable = BehaviorSubject.create<List<AttributeInputViewModel>>()

    val isBusyObservable = BehaviorSubject.createDefault<Boolean>(false)

    var isValid: Boolean = true
        protected set

    var isBusy: Boolean
        get() {
            return isBusyObservable.value
        }
        protected set(value) {
            if (isBusyObservable.value != value) {
                isBusyObservable.onNext(value)
            }
        }
    var onInitialized = PublishSubject.create<Pair<ItemMode, BuilderCreationMode?>>()
    protected val currentAttributeViewModelList = ArrayList<AttributeInputViewModel>()

    var mode: ItemMode
        get() {
            return modeObservable.value
        }
        protected set(value) {
            if (modeObservable.value != value) {
                modeObservable.onNext(value)
            }
        }

    fun init(trackerId: String, itemId: String?) {
        isValid = true
        if (trackerDao?.objectId != trackerId) {
            trackerDao = OTApp.instance.databaseManager.getTrackerQueryWithId(trackerId, realm).findFirst()
            trackerNameObservable.onNext(trackerDao?.name ?: "")
            subscriptions.clear()


            val unManagedTrackerDao = realm.copyFromRealm(trackerDao)!!

            currentAttributeViewModelList.forEach { it.unregister() }
            currentAttributeViewModelList.clear()

            currentAttributeViewModelList.addAll(unManagedTrackerDao.attributes.map { AttributeInputViewModel(it) })
            attributeViewModelListObservable.onNext(currentAttributeViewModelList)

            val initResult = onInit(trackerDao!!, itemId)

            if (initResult != null) {
                mode = initResult.first

                onInitialized.onNext(initResult)
            }
        }
    }

    abstract fun onInit(trackerDao: OTTrackerDAO, itemId: String?): Pair<ItemMode, BuilderCreationMode?>?

    open fun reset() {
        val trackerId = trackerDao?.objectId
        if (trackerId != null) {
            trackerDao = null
            init(trackerId, null)
        }
    }

    class AttributeInputViewModel(val attributeDAO: OTAttributeDAO) {
        val attributeLocalId: String get() = attributeDAO.localId
        val columnNameObservable: Observable<String> = BehaviorSubject.createDefault<String>("")
        val isRequiredObservable: Observable<Boolean> = BehaviorSubject.create<Boolean>()
        val stateObservable: Observable<OTItemBuilderWrapperBase.EAttributeValueState> = BehaviorSubject.create<OTItemBuilderWrapperBase.EAttributeValueState>()

        val valueObservable = BehaviorSubject.createDefault<Nullable<ValueWithTimestamp>>(Nullable(null) as Nullable<ValueWithTimestamp>)

        val validationObservable: Observable<Boolean> = BehaviorSubject.createDefault<Boolean>(true)

        var value: ValueWithTimestamp?
            get() = valueObservable.value?.datum
            set(value) {
                if (valueObservable.value?.datum != value) {
                    valueObservable.onNext(Nullable(value))
                    validateValue()
                }
            }

        var isValidated: Boolean
            get() = (validationObservable as BehaviorSubject).value
            internal set(value) {
                if ((validationObservable as BehaviorSubject).value != value) {
                    println("validation changed: ${attributeLocalId}, ${value}")
                    validationObservable.onNext(value)
                }
            }

        var isRequired: Boolean
            get() = (isRequiredObservable as BehaviorSubject<Boolean>).value
            internal set(value) {
                if (isRequired != value) {
                    (isRequiredObservable as BehaviorSubject<Boolean>).onNext(value)
                }
            }

        var state: OTItemBuilderWrapperBase.EAttributeValueState
            get() = (stateObservable as BehaviorSubject<OTItemBuilderWrapperBase.EAttributeValueState>).value
            internal set(value) {
                if ((stateObservable as BehaviorSubject<OTItemBuilderWrapperBase.EAttributeValueState>).value != value) {
                    stateObservable.onNext(value)
                }
            }

        private val subscriptions = CompositeDisposable()

        init {
            (columnNameObservable as BehaviorSubject<String>).onNext(attributeDAO.name)
            (isRequiredObservable as BehaviorSubject<Boolean>).onNext(attributeDAO.isRequired)
            validateValue()
        }

        fun unregister() {
            subscriptions.clear()
        }

        private fun validateValue() {
            isValidated = !(isRequired == true && value?.value == null)
        }

    }

    abstract fun startAutoComplete()

    abstract fun isViewModelsDirty(): Boolean

    protected open fun setValueOfAttribute(attributeLocalId: String, valueWithTimestamp: ValueWithTimestamp) {
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

    abstract fun cacheEditingInfo()

    abstract fun applyEditingToDatabase(): Maybe<String>

    abstract fun clearHistory()


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