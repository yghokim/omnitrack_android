package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Application
import android.os.Bundle
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kr.ac.snu.hcil.android.common.containers.AnyValueWithTimestamp
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.onNextIfDifferAndNotNull
import kr.ac.snu.hcil.android.common.view.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTDescriptionPanelDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTTrackerLayoutElementDAO
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.RealmViewModel
import javax.inject.Inject

/**
 * Created by Young-Ho on 10/15/2017.
 */
abstract class ItemEditionViewModelBase(app: Application) : RealmViewModel(app), OTItemBuilderWrapperBase.AttributeStateChangedListener {

    @Inject
    protected lateinit var syncManager: OTSyncManager

    lateinit var trackerDao: OTTrackerDAO
        protected set

    val hasTrackerRemovedOutside = BehaviorSubject.create<String>()
    val hasItemRemovedOutside = BehaviorSubject.create<String>()

    val trackerNameObservable = BehaviorSubject.create<String>()
    val schemaInformationObservable = BehaviorSubject.create<Triple<
            List<AttributeInputViewModel>,
            Array<OTDescriptionPanelDAO>?,
            Array<OTTrackerLayoutElementDAO>?
            >>()

    val isAllInputCompleteObservable = BehaviorSubject.createDefault(false)

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

    override fun onInject(app: OTAndroidApp) {
        app.applicationComponent.inject(this)
    }

    protected fun init(trackerId: String): Boolean {
        isValid = true
        if (!this::trackerDao.isInitialized || trackerDao._id != trackerId) {
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
                                hasTrackerRemovedOutside.onNext(unManagedTrackerDao._id!!)
                            }
                        }
                )

                currentAttributeViewModelList.forEach { it.unregister() }
                currentAttributeViewModelList.clear()

                currentAttributeViewModelList.addAll(unManagedTrackerDao.fields.asSequence().filter { !it.isHidden && !it.isInTrashcan }.map { AttributeInputViewModel(it) }.toList())
                schemaInformationObservable.onNext(Triple(currentAttributeViewModelList, unManagedTrackerDao.descriptionPanels?.toTypedArray(), unManagedTrackerDao.layout?.toTypedArray()))

                this.checkAllInputCompleteAndReturn()

                return true
            } else throw IllegalArgumentException("No such tracker.")
        } else return false
    }

    fun checkAllInputCompleteAndReturn(): Boolean {
        val isComplete = currentAttributeViewModelList.none { attributeViewModel ->
            attributeViewModel.fieldDAO.isValueValid(attributeViewModel.value?.value, getItemPivotTime())
        }

        this.isAllInputCompleteObservable.onNextIfDifferAndNotNull(isComplete)

        return isComplete
    }

    open fun onSaveInstanceState(outState: Bundle) {

    }

    inner class AttributeInputViewModel(unmanagedFieldDao: OTFieldDAO) : IReadonlyObjectId {
        override val _id: String? = unmanagedFieldDao._id

        val fieldLocalId: String = unmanagedFieldDao.localId
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
                    if (isRequired)
                        checkAllInputCompleteAndReturn()
                }
            }

        var isValidated: Boolean
            get() = (validationObservable as BehaviorSubject).value ?: true
            internal set(value) {
                if ((validationObservable as BehaviorSubject).value != value) {
                    println("validation changed: $fieldLocalId, $value")
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

        val onAttributeChanged = PublishSubject.create<OTFieldDAO>()

        val fieldDAO: OTFieldDAO = realm.where(OTFieldDAO::class.java).equalTo(BackendDbManager.FIELD_OBJECT_ID, unmanagedFieldDao._id).findFirst()!!

        init {

            fieldDAO.addChangeListener<OTFieldDAO> { newAttributeDao ->
                onAttributeChanged.onNext(newAttributeDao)
            }

            (columnNameObservable as BehaviorSubject<String>).onNext(fieldDAO.name)
            (isRequiredObservable as BehaviorSubject<Boolean>).onNext(fieldDAO.isRequired)
            validateValue()
        }

        fun unregister() {
            subscriptions.clear()
        }

        private fun validateValue() {
            isValidated = fieldDAO.isValueValid(value?.value, getItemPivotTime())
        }

    }

    abstract fun isViewModelsDirty(): Boolean

    protected open fun getItemPivotTime(): Long {
        return System.currentTimeMillis()
    }

    protected open fun setValueOfAttribute(fieldLocalId: String, valueWithTimestamp: AnyValueWithTimestamp) {
        val match = currentAttributeViewModelList.find { it.fieldLocalId == fieldLocalId }
        if (match != null) {
            match.value = valueWithTimestamp
        }
    }

    override fun onAttributeStateChanged(fieldLocalId: String, state: OTItemBuilderWrapperBase.EAttributeValueState) {
        val match = currentAttributeViewModelList.find { it.fieldLocalId == fieldLocalId }
        if (match != null) {
            match.state = state
        }
    }

    protected fun syncItemToServer() {
        syncManager.registerSyncQueue(ESyncDataType.ITEM, SyncDirection.UPLOAD, ignoreDirtyFlags = false)
    }

    abstract fun applyEditingToDatabase(): Maybe<String>

}