package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.app.Application
import android.support.annotation.DrawableRes
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.realm.Realm
import io.realm.RealmChangeListener
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.CreationFlagsHelper
import kr.ac.snu.hcil.omnitrack.core.LockedPropertiesHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.research.ExperimentInfo
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.research.OTExperimentDAO
import kr.ac.snu.hcil.omnitrack.core.di.configured.Research
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.RealmViewModel
import kr.ac.snu.hcil.omnitrack.utils.*
import org.jetbrains.anko.collections.forEachWithIndex
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashSet

/**
 * Created by younghokim on 2017. 10. 3..
 */
class TrackerDetailViewModel(app: Application) : RealmViewModel(app) {

    companion object {
        const val TAG = "TrackerDetailViewModel"
    }

    @Inject
    protected lateinit var attributeManager: Lazy<OTAttributeManager>

    @Inject
    protected lateinit var authManager: Lazy<OTAuthManager>

    @Inject
    protected lateinit var shortcutPanelManager: Lazy<OTShortcutPanelManager>

    @Inject
    protected lateinit var syncManager: OTSyncManager

    @field:[Inject Research]
    protected lateinit var researchRealmFactory: Factory<Realm>

    private lateinit var researchRealm: Realm

    private var isInitialized = false

    val isInitializedObservable = BehaviorSubject.create<Boolean>()

    val isEditMode: Boolean get() = trackerDao != null

    var trackerDao: OTTrackerDAO? = null
        private set

    private var initialSnapshotDao: OTTrackerDAO? = null

    var trackerId: String? = null
        private set

    private var experimentList = ArrayList<ExperimentInfo>()

    //Observables========================================
    val hasTrackerRemovedOutside = BehaviorSubject.create<String>()
    val trackerIdObservable = BehaviorSubject.createDefault<Nullable<String>>(Nullable(null))

    val reminderCountObservable = BehaviorSubject.createDefault<Int>(0)

    val nameObservable = BehaviorSubject.createDefault<String>("")
    val isBookmarkedObservable = BehaviorSubject.createDefault<Boolean>(false)
    val colorObservable = BehaviorSubject.createDefault<Int>(OTApp.instance.colorPalette[0])
    val attributeViewModelListObservable = BehaviorSubject.create<List<AttributeInformationViewModel>>()

    val areAttributesRemovable = BehaviorSubject.createDefault<Boolean>(true)

    val isInjectedObservable = BehaviorSubject.create<Boolean>()

    val experimentIdObservable = BehaviorSubject.create<Nullable<String>>()

    val experimentListObservable = BehaviorSubject.create<List<ExperimentInfo>>()

    //===================================================

    private var lastRemovedAttributeId: String? = null

    var name: String
        get() = nameObservable.value ?: ""
        set(value) {
            if (trackerDao != null) {
                realm.executeTransactionIfNotIn {
                    trackerDao?.name = value
                    trackerDao?.synchronizedAt = null
                }
                registerSyncJob()
            } else if (value != nameObservable.value) {
                nameObservable.onNext(value)
            }
        }

    var isBookmarked: Boolean
        get() = isBookmarkedObservable.value ?: false
        set(value) {
            if (trackerDao != null) {
                realm.executeTransactionIfNotIn {
                    trackerDao?.isBookmarked = value
                    trackerDao?.synchronizedAt = null
                }
                registerSyncJob()
            } else if (value != isBookmarkedObservable.value) {
                isBookmarkedObservable.onNext(value)
            }
        }

    var color: Int
        get() = colorObservable.value ?: 0
        set(value) {
            if (trackerDao != null) {
                realm.executeTransactionIfNotIn {
                    trackerDao?.color = value
                    trackerDao?.synchronizedAt = null
                }
                registerSyncJob()
            } else if (value != colorObservable.value) {
                colorObservable.onNext(value)
            }
        }

    var assignedExperimentId: String?
        get() = experimentIdObservable.value?.datum
        set(value) {
            if (trackerDao != null) {
                if (!CreationFlagsHelper.isInjected(trackerDao!!.getParsedCreationFlags())) {
                    realm.executeTransactionIfNotIn {
                        trackerDao?.serializedCreationFlags = CreationFlagsHelper.Builder(trackerDao?.serializedCreationFlags
                                ?: "{}").setExperiment(value).build()
                        trackerDao?.clearCreationFlagsCache()
                        trackerDao?.synchronizedAt = null
                    }
                    registerSyncJob()
                }
            } else {
                experimentIdObservable.onNextIfDifferAndNotNull(Nullable(value))
            }
        }

    val onChangesApplied = PublishSubject.create<String>()

    private val removedAttributes = HashSet<AttributeInformationViewModel>()

    private val currentAttributeViewModelList = ArrayList<AttributeInformationViewModel>()

    fun registerSyncJob() {
        syncManager.registerSyncQueue(ESyncDataType.TRACKER, SyncDirection.UPLOAD, ignoreDirtyFlags = false)
    }

    override fun onInject(configuredContext: ConfiguredContext) {
        configuredContext.configuredAppComponent.inject(this)
        researchRealm = researchRealmFactory.get()
    }

    fun init(trackerId: String?) {
        if (!isInitialized) {
            removedAttributes.clear()
            lastRemovedAttributeId = null
            subscriptions.clear()
            if (trackerId != null) {
                this.trackerId = trackerId
                val dao = dbManager.get().getTrackerQueryWithId(trackerId, realm).findFirstAsync()

                subscriptions.add(
                        dao.asFlowable<OTTrackerDAO>().filter { it.isLoaded }.subscribe { snapshot ->
                            if (snapshot.isValid) {
                                if (initialSnapshotDao == null)
                                    initialSnapshotDao = realm.copyFromRealm(snapshot)

                                nameObservable.onNextIfDifferAndNotNull(snapshot.name)
                                isBookmarkedObservable.onNextIfDifferAndNotNull(snapshot.isBookmarked)
                                colorObservable.onNextIfDifferAndNotNull(snapshot.color)
                                areAttributesRemovable.onNextIfDifferAndNotNull(!(LockedPropertiesHelper.isLocked(LockedPropertiesHelper.TRACKER_REMOVE_ATTRIBUTES, snapshot.getParsedLockedPropertyInfo())
                                        ?: false))

                                isInjectedObservable.onNextIfDifferAndNotNull(CreationFlagsHelper.isInjected(snapshot.getParsedCreationFlags()))
                                experimentIdObservable.onNextIfDifferAndNotNull(Nullable(CreationFlagsHelper.getExperimentId(snapshot.getParsedCreationFlags())))

                                if (trackerDao != dao) {
                                    subscriptions.add(
                                            snapshot.makeAttributesQuery(false, null).findAllAsync().asChangesetObservable().subscribe { changes ->
                                                val changeset = changes.changeset
                                                if (changeset == null) {
                                                    //initial
                                                    clearCurrentAttributeList()
                                                    currentAttributeViewModelList.addAll(changes.collection.map { AttributeInformationViewModel(it, realm, attributeManager.get()) })
                                                } else {
                                                    currentAttributeViewModelList.removeAll(
                                                            changeset.deletions.map { currentAttributeViewModelList[it].apply { this.unregister() } }
                                                    )

                                                    changeset.insertions.forEach {
                                                        currentAttributeViewModelList.add(it,
                                                                AttributeInformationViewModel(changes.collection[it]!!, realm, attributeManager.get())
                                                        )
                                                    }
                                                }

                                                attributeViewModelListObservable.onNext(currentAttributeViewModelList)
                                            }
                                    )


                                    snapshot.liveTriggersQuery?.let {
                                        subscriptions.add(
                                                it.equalTo("actionType", OTTriggerDAO.ACTION_TYPE_REMIND)
                                                        .findAllAsync()
                                                        .asFlowable()
                                                        .subscribe {
                                                            reminderCountObservable.onNextIfDifferAndNotNull(it.size)
                                                        }
                                        )
                                    }

                                    trackerDao = dao
                                }
                            } else if (trackerDao != null) {
                                //the tracker was removed outside
                                hasTrackerRemovedOutside.onNext(trackerId)
                            }
                        }
                )

                shortcutPanelManager.get().registerShortcutRefreshSubscription(authManager.get().userId!!, TAG)

                /*
                attributeRealmResults?.removeChangeListener(attributeListChangedListener)
                attributeRealmResults = OTApp.instance.databaseManager.getAttributeListQuery(trackerId, realm).findAllSortedAsync("position")
                attributeRealmResults?.addChangeListener(attributeListChangedListener)*/

            } else {
                trackerDao = null
                isInjectedObservable.onNextIfDifferAndNotNull(false)
            }

            subscriptions.add(
                    researchRealm.where(OTExperimentDAO::class.java)
                            .isNull("droppedAt")
                            .findAllAsync().asFlowable()
                            .filter {
                                it.isValid && it.isLoaded
                            }
                            .subscribe { results ->
                                this.experimentList.clear()
                                this.experimentList.addAll(results.map { it.getInfo() })
                                this.experimentListObservable.onNext(this.experimentList)
                            }
            )

            trackerIdObservable.onNext(Nullable(trackerId))

            isInitialized = true
            isInitializedObservable.onNext(true)
        }
    }

    fun addNewAttribute(name: String, type: Int, processor: ((OTAttributeDAO, Realm) -> OTAttributeDAO)? = null) {
        val newDao = OTAttributeDAO()
        newDao.objectId = UUID.randomUUID().toString()
        newDao.name = name
        newDao.type = type
        newDao.trackerId = trackerId
        newDao.initialize(configuredContext)
        processor?.invoke(newDao, realm)

        if (trackerDao != null) {
            newDao.localId = attributeManager.get().makeNewAttributeLocalId(newDao.userCreatedAt)
            newDao.trackerId = trackerDao?.objectId

            realm.executeTransactionIfNotIn {
                trackerDao?.attributes?.add(newDao)
            }
        } else {
            currentAttributeViewModelList.add(AttributeInformationViewModel(newDao, realm, attributeManager.get()))
            attributeViewModelListObservable.onNext(currentAttributeViewModelList)
        }
    }

    fun moveAttribute(from: Int, to: Int) {
        if (trackerDao != null) {
            val attributes = currentAttributeViewModelList.map { it.attributeDAO }.toMutableList()
            attributes.move(from, to)
            realm.executeTransactionIfNotIn {
                trackerDao?.attributes?.removeAll(attributes)
                trackerDao?.attributes?.addAll(0, attributes)
                trackerDao?.synchronizedAt = null
            }
            registerSyncJob()
        } else {
            currentAttributeViewModelList.move(from, to)
            attributeViewModelListObservable.onNext(currentAttributeViewModelList)
        }
    }

    fun removeAttribute(attrViewModel: AttributeInformationViewModel) {
        if (trackerDao != null) {
            lastRemovedAttributeId = attrViewModel.objectId
            realm.executeTransactionIfNotIn {
                val attribute = trackerDao?.attributes?.find { it.objectId == attrViewModel.objectId }
                if (attribute != null) {
                    attribute.isInTrashcan = true
                }
                trackerDao?.synchronizedAt = null
            }
            registerSyncJob()
        } else {
            removedAttributes.add(attrViewModel)
            currentAttributeViewModelList.remove(attrViewModel)
            attributeViewModelListObservable.onNext(currentAttributeViewModelList)
        }
    }

    fun undoRemove(): Completable {
        return Completable.defer {

            if (isEditMode) {
                if (lastRemovedAttributeId != null) {
                    val trackerId = trackerId
                    return@defer realm.executeTransactionAsObservable { realm ->
                        val removedItem = realm.where(OTAttributeDAO::class.java).equalTo(BackendDbManager.FIELD_OBJECT_ID, lastRemovedAttributeId!!).findFirst()
                        if (removedItem != null) {
                            removedItem.isInTrashcan = false

                            val trackerDao = dbManager.get().getTrackerQueryWithId(trackerId!!, realm).findFirst()
                            if (trackerDao?.synchronizedAt != null) {
                                trackerDao.synchronizedAt = null
                                registerSyncJob()
                            }
                        }
                    }.doAfterTerminate {
                        lastRemovedAttributeId = null
                    }
                } else return@defer Completable.complete()
            } else {
                if (removedAttributes.isNotEmpty()) {
                    val last = removedAttributes.last()
                    removedAttributes.remove(last)
                    currentAttributeViewModelList.add(last)
                    attributeViewModelListObservable.onNext(currentAttributeViewModelList)
                }
                return@defer Completable.complete()
            }
        }
    }

    val isNameDirty: Boolean get() = if (initialSnapshotDao == null) false else initialSnapshotDao?.name != nameObservable.value
    val isBookmarkedDirty: Boolean get() = if (initialSnapshotDao == null) false else initialSnapshotDao?.isBookmarked != isBookmarkedObservable.value
    val isColorDirty: Boolean get() = if (initialSnapshotDao == null) false else initialSnapshotDao?.color != colorObservable.value

    private val areAttributesDirty: Boolean
        get() {
            return if (initialSnapshotDao == null) false else (currentAttributeViewModelList.find { it.isDirty == true } != null) ||
                    !Arrays.equals(initialSnapshotDao?.attributes?.map { attr -> attr.objectId }?.toTypedArray(), currentAttributeViewModelList.map { it.attributeDAO.objectId }.toTypedArray())
        }

    val isDirty: Boolean
        get() {
            return if (initialSnapshotDao == null) false
            else isNameDirty || isBookmarkedDirty || isColorDirty || areAttributesDirty
        }

    fun applyChanges(): String {
        if (trackerDao == null) {
            realm.executeTransaction {
                val trackerDao = realm.createObject(OTTrackerDAO::class.java, UUID.randomUUID().toString())
                trackerDao.userId = authManager.get().userId
                trackerDao.name = nameObservable.value ?: ""
                trackerDao.isBookmarked = isBookmarkedObservable.value ?: false
                trackerDao.color = colorObservable.value ?: 0

                if (isInjectedObservable.value != true) {
                    trackerDao.serializedCreationFlags = CreationFlagsHelper.Builder().setExperiment(assignedExperimentId).build()
                }

                currentAttributeViewModelList.forEachWithIndex { index, attrViewModel ->
                    if (!attrViewModel.isInDatabase) {
                        attrViewModel.attributeDAO.localId = attributeManager.get().makeNewAttributeLocalId(attrViewModel.attributeDAO.userCreatedAt)
                        attrViewModel.attributeDAO.trackerId = trackerDao.objectId
                        attrViewModel.saveToRealm()
                    }
                    attrViewModel.attributeDAO.position = index
                    attrViewModel.applyChanges()
                    trackerDao.attributes.add(attrViewModel.attributeDAO)
                }

                removedAttributes.clear()

                trackerDao.synchronizedAt = null

                this.trackerDao = trackerDao
            }

            currentAttributeViewModelList.forEach {
                it.register()
            }

            registerSyncJob()
        }

        onChangesApplied.onNext(trackerDao!!.objectId!!)

        return trackerDao!!.objectId!!
    }

    fun clearTrackerSynchronizationFlag() {
        realm.executeTransactionIfNotIn {
            trackerDao?.synchronizedAt = null
        }
    }

    private fun clearCurrentAttributeList() {
        currentAttributeViewModelList.forEach {
            it.unregister()
        }
        currentAttributeViewModelList.clear()
    }


    override fun onCleared() {
        super.onCleared()
        clearCurrentAttributeList()
        removedAttributes.clear()
        shortcutPanelManager.get().unregisterShortcutRefreshSubscription(TAG)
        if (this::researchRealm.isInitialized) {
            researchRealm.close()
        }
    }

    class AttributeInformationViewModel(_attributeDAO: OTAttributeDAO, val realm: Realm, val attributeManager: OTAttributeManager) : IReadonlyObjectId, RealmChangeListener<OTAttributeDAO> {
        override val objectId: String? = _attributeDAO.objectId

        var attributeDAO: OTAttributeDAO = _attributeDAO
            private set

        val isInDatabase: Boolean get() = attributeDAO.isManaged

        val isEditable = BehaviorSubject.createDefault<Boolean>(true)
        val isRemovable = BehaviorSubject.createDefault<Boolean>(true)
        val isVisibilityEditable = BehaviorSubject.createDefault<Boolean>(true)

        val nameObservable = BehaviorSubject.createDefault<String>("")
        val isRequiredObservable = BehaviorSubject.createDefault<Boolean>(false)
        val typeObservable = BehaviorSubject.createDefault<Int>(-1)
        val iconObservable = BehaviorSubject.createDefault<Int>(R.drawable.icon_small_longtext)
        val connectionObservable = BehaviorSubject.create<Nullable<OTConnection>>()
        val defaultValuePolicyObservable = BehaviorSubject.createDefault<Int>(OTAttributeDAO.DEFAULT_VALUE_POLICY_NULL)
        val defaultValuePresetObservable = BehaviorSubject.createDefault<Nullable<String>>(Nullable<String>(null))

        val isHiddenObservable = BehaviorSubject.createDefault(false)

        val onPropertyChanged = PublishSubject.create<Long>()

        val internalSubscription = CompositeDisposable()

        //key, dbId/serializedValue
        private val propertyTable = Hashtable<String, Pair<String, String>>()

        var name: String
            get() = nameObservable.value ?: ""
            set(value) {
                if (nameObservable.value != value) {
                    nameObservable.onNext(value)
                }
            }

        var icon: Int
            get() = iconObservable.value ?: 0
            set(@DrawableRes value) {
                if (iconObservable.value != value) {
                    iconObservable.onNext(value)
                }
            }

        var isRequired: Boolean
            get() = isRequiredObservable.value ?: false
            set(value) {
                if (isRequiredObservable.value != value) {
                    isRequiredObservable.onNext(value)
                }
            }

        var isHidden: Boolean
            get() = isHiddenObservable.value ?: false
            set(value) {
                if (isHiddenObservable.value != value) {
                    isHiddenObservable.onNext(value)
                }
            }

        var defaultValuePolicy: Int
            get() = defaultValuePolicyObservable.value ?: OTAttributeDAO.DEFAULT_VALUE_POLICY_NULL
            set(value) {
                if (value != defaultValuePolicyObservable.value) {
                    defaultValuePolicyObservable.onNext(value)
                }
            }

        var defaultValuePreset: String?
            get() = defaultValuePresetObservable.value?.datum
            set(value) {
                defaultValuePresetObservable.onNextIfDifferAndNotNull(Nullable(value))
            }

        init {
            if (isInDatabase)
                attributeDAO.addChangeListener(this)

            onChange(attributeDAO)

        }

        fun unregister() {
            if (isInDatabase)
                attributeDAO.removeChangeListener(this)

            internalSubscription.clear()
        }

        override fun onChange(snapshot: OTAttributeDAO) {
            if (snapshot.isValid) {
                applyDaoChangesToFront(snapshot)
            }
        }

        private fun arePropertiesDirty(): Boolean {
            for (entry in propertyTable) {
                if (attributeDAO.getPropertySerializedValue(entry.key) != entry.value.second)
                    return true
            }
            return false
        }

        val isDirty: Boolean
            get() {
                return attributeDAO.localId.isBlank()
                        || attributeDAO.name != name
                        || attributeDAO.isRequired != isRequired
                        || attributeDAO.fallbackValuePolicy != defaultValuePolicy
                        || attributeDAO.fallbackPresetSerializedValue != defaultValuePreset
                        || attributeDAO.serializedConnection != connectionObservable.value?.datum?.getSerializedString()
                        || attributeDAO.isHidden != isHidden
                        || arePropertiesDirty()
            }

        fun makeFrontalChangesToDao(): OTAttributeDAO {
            val dao = OTAttributeDAO()
            dao.objectId = attributeDAO.objectId
            dao.type = attributeDAO.type
            dao.localId = attributeDAO.localId
            dao.position = attributeDAO.position
            dao.trackerId = attributeDAO.trackerId
            dao.userUpdatedAt = attributeDAO.userUpdatedAt

            dao.isRequired = this.isRequired
            dao.fallbackValuePolicy = this.defaultValuePolicy
            dao.fallbackPresetSerializedValue = this.defaultValuePreset
            dao.isHidden = this.isHidden
            dao.name = name
            for (entry in propertyTable) {
                dao.setPropertySerializedValue(entry.key, entry.value.second)
            }

            dao.serializedConnection = connectionObservable.value?.datum?.getSerializedString()

            return dao
        }

        fun applyDaoChangesToFront(editedDao: OTAttributeDAO) {
            name = editedDao.name

            isRequired = editedDao.isRequired

            isEditable.onNextIfDifferAndNotNull(!editedDao.isEditingLocked())
            isRemovable.onNextIfDifferAndNotNull(!editedDao.isDeletionLocked())
            isVisibilityEditable.onNextIfDifferAndNotNull(!editedDao.isVisibilityLocked())

            defaultValuePolicy = editedDao.fallbackValuePolicy
            defaultValuePreset = editedDao.fallbackPresetSerializedValue
            isHidden = editedDao.isHidden

            println("serialized connection of dao: ${editedDao.serializedConnection}")
            editedDao.serializedConnection?.let {
                val connection = OTConnection.fromJson(it)
                if (connectionObservable.value?.datum != connection) {
                    connectionObservable.onNext(Nullable(connection))
                }
            } ?: if (connectionObservable.value?.datum != null) {
                connectionObservable.onNext(Nullable(null))
            }

            propertyTable.clear()
            var changed = false
            editedDao.properties.forEach { entry ->
                if (propertyTable[entry.key] != Pair(entry.id, entry.value!!)) {
                    propertyTable[entry.key] = Pair(entry.id, entry.value!!)
                    changed = true
                }
            }

            val helper = attributeManager.getAttributeHelper(editedDao.type)
            icon = helper.getTypeSmallIconResourceId(editedDao)
            if (changed) {
                onPropertyChanged.onNext(System.currentTimeMillis())
            }

            if (typeObservable.value != editedDao.type) {
                typeObservable.onNext(editedDao.type)
            }
        }

        fun applyChanges() {
            realm.executeTransactionIfNotIn {
                attributeDAO.name = name
                attributeDAO.isHidden = isHidden
                attributeDAO.isRequired = isRequired
                attributeDAO.userUpdatedAt = System.currentTimeMillis()
                attributeDAO.fallbackValuePolicy = defaultValuePolicy
                attributeDAO.fallbackPresetSerializedValue = defaultValuePreset
                for (entry in propertyTable) {
                    println("set new serialized value: $entry")
                    attributeDAO.setPropertySerializedValue(entry.key, entry.value.second)
                    println("get serialized value: ${attributeDAO.getPropertySerializedValue(entry.key)}")
                }
                attributeDAO.serializedConnection = connectionObservable.value?.datum?.getSerializedString()
            }
        }

        fun saveToRealm() {
            this.attributeDAO = realm.copyToRealm(this.attributeDAO)
        }

        fun register() {
            if (isInDatabase)
                attributeDAO.addChangeListener(this)
        }
    }
}