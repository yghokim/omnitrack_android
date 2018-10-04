package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Application
import android.os.Bundle
import com.google.gson.JsonObject
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.RealmViewModel
import kr.ac.snu.hcil.omnitrack.utils.AnyValueWithTimestamp
import kr.ac.snu.hcil.omnitrack.utils.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import javax.inject.Inject

/**
 * Created by Young-Ho on 10/15/2017.
 */
abstract class ItemEditionViewModelBase(app: Application) : RealmViewModel(app), OTItemBuilderWrapperBase.AttributeStateChangedListener {

    @Inject
    protected lateinit var syncManager: OTSyncManager

    lateinit var metadataForItem: JsonObject
        protected set

    lateinit var trackerDao: OTTrackerDAO
        protected set

    val hasTrackerRemovedOutside = BehaviorSubject.create<String>()
    val hasItemRemovedOutside = BehaviorSubject.create<String>()

    val trackerNameObservable = BehaviorSubject.create<String>()
    val attributeViewModelListObservable = BehaviorSubject.create<List<AttributeInputViewModel>>()

    val isBusyObservable = BehaviorSubject.createDefault<Boolean>(false)

    var isValid: Boolean = true
        protected set

    var isBusy: Boolean
        get() {
            return isBusyObservable.value ?: false
        }
        protected set(value) {
            if (isBusyObservable.value != value) {
                isBusyObservable.onNext(value)
            }
        }

    protected val currentAttributeViewModelList = ArrayList<AttributeInputViewModel>()

    override fun onInject(configuredContext: ConfiguredContext) {
        configuredContext.configuredAppComponent.inject(this)
    }

    protected fun init(trackerId: String): Boolean {
        isValid = true
        if (!this::trackerDao.isInitialized || trackerDao.objectId != trackerId) {
            /*
            this.metadataForItem = metadata
            if (!this.metadataForItem.has("screenAccessedAt")) {
                this.metadataForItem.addProperty("screenAccessedAt", System.currentTimeMillis())
            }*/

            val trackerDao = dbManager.get().getTrackerQueryWithId(trackerId, realm).findFirst()
            if (trackerDao != null) {
                this.trackerDao = trackerDao
                trackerNameObservable.onNext(trackerDao.name)
                subscriptions.clear()

                val unManagedTrackerDao = realm.copyFromRealm(trackerDao)!!

                subscriptions.add(
                        trackerDao.asFlowable<OTTrackerDAO>().filter { it.isLoaded }.subscribe { dao ->
                            if (!dao.isValid) {
                                hasTrackerRemovedOutside.onNext(unManagedTrackerDao.objectId!!)
                            }
                        }
                )

                currentAttributeViewModelList.forEach { it.unregister() }
                currentAttributeViewModelList.clear()

                currentAttributeViewModelList.addAll(unManagedTrackerDao.attributes.filter { !it.isHidden && !it.isInTrashcan }.map { AttributeInputViewModel(it) })
                attributeViewModelListObservable.onNext(currentAttributeViewModelList)
                return true
            } else throw IllegalArgumentException("No such tracker.")
        } else return false
    }

    open fun onSaveInstanceState(outState: Bundle) {

    }

    inner class AttributeInputViewModel(unmanagedAttributeDao: OTAttributeDAO) : IReadonlyObjectId {
        override val objectId: String? = unmanagedAttributeDao.objectId

        val attributeLocalId: String = unmanagedAttributeDao.localId
        val columnNameObservable: Observable<String> = BehaviorSubject.createDefault<String>("")
        val isRequiredObservable: Observable<Boolean> = BehaviorSubject.create<Boolean>()
        val stateObservable: Observable<OTItemBuilderWrapperBase.EAttributeValueState> = BehaviorSubject.create<OTItemBuilderWrapperBase.EAttributeValueState>()

        val valueObservable = BehaviorSubject.createDefault<Nullable<AnyValueWithTimestamp>>(Nullable(null) as Nullable<AnyValueWithTimestamp>)

        val validationObservable: Observable<Boolean> = BehaviorSubject.createDefault<Boolean>(true)

        var value: AnyValueWithTimestamp?
            get() = valueObservable.value?.datum
            set(value) {
                if (valueObservable.value?.datum != value) {
                    valueObservable.onNext(Nullable(value))
                    validateValue()
                }
            }

        var isValidated: Boolean
            get() = (validationObservable as BehaviorSubject).value ?: true
            internal set(value) {
                if ((validationObservable as BehaviorSubject).value != value) {
                    println("validation changed: ${attributeLocalId}, ${value}")
                    validationObservable.onNext(value)
                }
            }

        var isRequired: Boolean
            get() = (isRequiredObservable as BehaviorSubject<Boolean>).value ?: false
            internal set(value) {
                if (isRequired != value) {
                    (isRequiredObservable as BehaviorSubject<Boolean>).onNext(value)
                }
            }

        var state: OTItemBuilderWrapperBase.EAttributeValueState
            get() = (stateObservable as BehaviorSubject<OTItemBuilderWrapperBase.EAttributeValueState>).value
                    ?: OTItemBuilderWrapperBase.EAttributeValueState.Idle
            internal set(value) {
                if ((stateObservable as BehaviorSubject<OTItemBuilderWrapperBase.EAttributeValueState>).value != value) {
                    stateObservable.onNext(value)
                }
            }

        private val subscriptions = CompositeDisposable()

        val onAttributeChanged = PublishSubject.create<OTAttributeDAO>()

        val attributeDAO: OTAttributeDAO = realm.where(OTAttributeDAO::class.java).equalTo(BackendDbManager.FIELD_OBJECT_ID, unmanagedAttributeDao.objectId).findFirst()!!

        init {

            attributeDAO.addChangeListener<OTAttributeDAO> { newAttributeDao ->
                onAttributeChanged.onNext(newAttributeDao)
            }

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

    abstract fun isViewModelsDirty(): Boolean

    protected open fun setValueOfAttribute(attributeLocalId: String, valueWithTimestamp: AnyValueWithTimestamp) {
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

    protected fun syncItemToServer() {
        syncManager.registerSyncQueue(ESyncDataType.ITEM, SyncDirection.UPLOAD, ignoreDirtyFlags = false)
    }

    abstract fun applyEditingToDatabase(): Maybe<String>

}