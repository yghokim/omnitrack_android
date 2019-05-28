package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.app.Application
import android.os.Bundle
import androidx.annotation.DrawableRes
import com.google.gson.JsonObject
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.realm.Realm
import io.realm.RealmChangeListener
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.move
import kr.ac.snu.hcil.android.common.onNextIfDifferAndNotNull
import kr.ac.snu.hcil.android.common.view.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.research.ExperimentInfo
import kr.ac.snu.hcil.omnitrack.core.database.typeadapters.ServerCompatibleTypeAdapter
import kr.ac.snu.hcil.omnitrack.core.di.global.ForTracker
import kr.ac.snu.hcil.omnitrack.core.di.global.Research
import kr.ac.snu.hcil.omnitrack.core.flags.CreationFlagsHelper
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.RealmViewModel
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionAsObservable
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import kr.ac.snu.hcil.omnitrack.views.color.ColorHelper
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

    @Inject
    protected lateinit var connectionTypeAdapter: Lazy<OTConnection.ConnectionTypeAdapter>

    @field:[Inject ForTracker]
    protected lateinit var trackerTypeAdapter: Lazy<ServerCompatibleTypeAdapter<OTTrackerDAO>>

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

    //Observables========================================
    val hasTrackerRemovedOutside = BehaviorSubject.create<String>()
    val trackerIdObservable = BehaviorSubject.createDefault<Nullable<String>>(Nullable(null))

    val reminderCountObservable = BehaviorSubject.createDefault(0)

    val nameObservable = BehaviorSubject.createDefault<String>("")
    val isBookmarkedObservable = BehaviorSubject.createDefault<Boolean>(false)

    lateinit var colorObservable: BehaviorSubject<Int>
        private set

    val attributeViewModelListObservable = BehaviorSubject.create<List<AttributeInformationViewModel>>()

    val lockedPropertiesObservable = BehaviorSubject.create<Nullable<JsonObject>>()

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
                        trackerDao?.experimentIdInFlags = value
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

    override fun onInject(app: OTAndroidApp) {
        app.applicationComponent.inject(this)
        colorObservable = BehaviorSubject.createDefault<Int>(ColorHelper.getTrackerColorPalette(getApplication())[0])
        researchRealm = researchRealmFactory.get()
    }

    fun init(trackerId: String?, savedInstanceState: Bundle?) {
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

                                //Locked properties ========
                                lockedPropertiesObservable.onNextIfDifferAndNotNull(Nullable(snapshot.getParsedLockedPropertyInfo()))

                                //==========================

                                isInjectedObservable.onNextIfDifferAndNotNull(CreationFlagsHelper.isInjected(snapshot.getParsedCreationFlags()))
                                experimentIdObservable.onNextIfDifferAndNotNull(Nullable(CreationFlagsHelper.getExperimentId(snapshot.getParsedCreationFlags())))

                                if (trackerDao != dao) {
                                    subscriptions.add(
                                            snapshot.makeAttributesQuery(false, null).findAllAsync().asChangesetObservable().subscribe { changes ->
                                                val changeset = changes.changeset
                                                if (changeset == null) {
                                                    //initial
                                                    clearCurrentAttributeList()
                                                    currentAttributeViewModelList.addAll(changes.collection.map { AttributeInformationViewModel(it, realm, attributeManager.get(), connectionTypeAdapter.get()) })
                                                } else {
                                                    currentAttributeViewModelList.removeAll(
                                                            changeset.deletions.map { currentAttributeViewModelList[it].apply { this.unregister() } }
                                                    )

                                                    changeset.insertions.forEach {
                                                        currentAttributeViewModelList.add(it,
                                                                AttributeInformationViewModel(changes.collection[it]!!, realm, attributeManager.get(), connectionTypeAdapter.get())
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

            if (savedInstanceState != null) {
                val serializedStateDao = savedInstanceState.getString("currentDao")
                if (serializedStateDao != null) {
                    val stateDao = trackerTypeAdapter.get().fromJson(serializedStateDao)

                    nameObservable.onNextIfDifferAndNotNull(stateDao.name)
                    isBookmarkedObservable.onNextIfDifferAndNotNull(stateDao.isBookmarked)
                    colorObservable.onNextIfDifferAndNotNull(stateDao.color)
                    //Locked properties ========
                    lockedPropertiesObservable.onNextIfDifferAndNotNull(Nullable(stateDao.getParsedLockedPropertyInfo()))
                    //==========================
                    isInjectedObservable.onNextIfDifferAndNotNull(CreationFlagsHelper.isInjected(stateDao.getParsedCreationFlags()))
                    experimentIdObservable.onNextIfDifferAndNotNull(Nullable(CreationFlagsHelper.getExperimentId(stateDao.getParsedCreationFlags())))

                    val newList = stateDao.attributes.map {
                        AttributeInformationViewModel(it, realm, attributeManager.get(), connectionTypeAdapter.get())
                    }
                    currentAttributeViewModelList.addAll(
                            newList
                    )

                    attributeViewModelListObservable.onNext(newList)
                }
            }

            trackerIdObservable.onNext(Nullable(trackerId))

            isInitialized = true
            isInitializedObservable.onNext(true)
        }
    }

    fun saveInstanceState(out: Bundle) {
        val currentDao = makeUnmanagedTrackerDaoFromSettings()
        out.putString("currentDao", trackerTypeAdapter.get().toJson(currentDao))
    }

    fun addNewAttribute(name: String, type: Int, processor: ((OTAttributeDAO, Realm) -> OTAttributeDAO)? = null) {
        val newDao = OTAttributeDAO()
        newDao._id = UUID.randomUUID().toString()
        newDao.name = name
        newDao.type = type
        newDao.trackerId = trackerId
        newDao.initialize(getApplication<OTApp>())
        processor?.invoke(newDao, realm)

        if (trackerDao != null) {
            newDao.localId = attributeManager.get().makeNewAttributeLocalId(newDao.userCreatedAt)
            newDao.trackerId = trackerDao?._id

            realm.executeTransactionIfNotIn {
                trackerDao?.attributes?.add(newDao)
                trackerDao?.synchronizedAt = null
            }
            registerSyncJob()
        } else {
            currentAttributeViewModelList.add(AttributeInformationViewModel(newDao, realm, attributeManager.get(), connectionTypeAdapter.get()))
            attributeViewModelListObservable.onNext(currentAttributeViewModelList)
        }
    }

    fun moveField(from: Int, to: Int) {
        if (trackerDao != null) {
            val attributes = currentAttributeViewModelList.asSequence().map { it.attributeDAO }.toMutableList()
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
            lastRemovedAttributeId = attrViewModel._id
            realm.executeTransactionIfNotIn {
                val attribute = trackerDao?.attributes?.find { it._id == attrViewModel._id }
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
                    !Arrays.equals(initialSnapshotDao?.attributes?.map { attr -> attr._id }?.toTypedArray(), currentAttributeViewModelList.map { it.attributeDAO._id }.toTypedArray())
        }

    val isDirty: Boolean
        get() {
            return if (initialSnapshotDao == null) false
            else isNameDirty || isBookmarkedDirty || isColorDirty || areAttributesDirty
        }

    private fun makeUnmanagedTrackerDaoFromSettings(): OTTrackerDAO {
        val trackerDao = OTTrackerDAO()
        trackerDao._id = UUID.randomUUID().toString()
        trackerDao.userId = authManager.get().userId
        trackerDao.name = nameObservable.value ?: ""
        trackerDao.isBookmarked = isBookmarkedObservable.value ?: false
        trackerDao.color = colorObservable.value ?: 0

        if (!BuildConfig.DEFAULT_EXPERIMENT_ID.isNullOrBlank()) {
            trackerDao.experimentIdInFlags = BuildConfig.DEFAULT_EXPERIMENT_ID
            trackerDao.serializedCreationFlags = CreationFlagsHelper.Builder().setExperiment(BuildConfig.DEFAULT_EXPERIMENT_ID).build()
        } else if (isInjectedObservable.value != true) {
            trackerDao.serializedCreationFlags = CreationFlagsHelper.Builder().setExperiment(assignedExperimentId).build()
        }

        currentAttributeViewModelList.forEachWithIndex { index, attrViewModel ->
            attrViewModel.applyChanges()
            trackerDao.attributes.add(attrViewModel.attributeDAO)
        }

        return trackerDao
    }

    fun applyChanges(): String {
        if (trackerDao == null) {
            removedAttributes.clear()
            val trackerDao = makeUnmanagedTrackerDaoFromSettings()

            currentAttributeViewModelList.forEachWithIndex { index, attrViewModel ->
                attrViewModel.attributeDAO.localId = attributeManager.get().makeNewAttributeLocalId(attrViewModel.attributeDAO.userCreatedAt)
                attrViewModel.attributeDAO.trackerId = trackerDao._id
                attrViewModel.attributeDAO.position = index
            }

            realm.executeTransaction {
                it.copyToRealm(trackerDao)
            }

            this.trackerDao = trackerDao

            currentAttributeViewModelList.forEach {
                it.register()
            }

            registerSyncJob()
        }

        onChangesApplied.onNext(trackerDao!!._id!!)

        return trackerDao!!._id!!
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

    class AttributeInformationViewModel(_attributeDAO: OTAttributeDAO, val realm: Realm, val attributeManager: OTAttributeManager, val connectionTypeAdapter: OTConnection.ConnectionTypeAdapter) : IReadonlyObjectId, RealmChangeListener<OTAttributeDAO> {
        override val _id: String? = _attributeDAO._id

        var attributeDAO: OTAttributeDAO = _attributeDAO
            private set

        val isInDatabase: Boolean get() = attributeDAO.isManaged

        val isEditable = BehaviorSubject.createDefault<Boolean>(true)
        val isRemovable = BehaviorSubject.createDefault<Boolean>(true)
        val isVisibilityEditable = BehaviorSubject.createDefault<Boolean>(true)

        val nameObservable = BehaviorSubject.createDefault<String>("")
        val isRequiredObservable = BehaviorSubject.createDefault<Boolean>(false)
        val typeObservable = BehaviorSubject.createDefault(-1)
        val iconObservable = BehaviorSubject.createDefault<Int>(R.drawable.icon_small_longtext)
        val connectionObservable = BehaviorSubject.create<Nullable<OTConnection>>()
        val defaultValuePolicyObservable = BehaviorSubject.createDefault<Int>(OTAttributeDAO.DEFAULT_VALUE_POLICY_NULL)
        val defaultValuePresetObservable = BehaviorSubject.createDefault<Nullable<String>>(Nullable(null))

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
                        || attributeDAO.serializedConnection != connectionObservable.value?.datum?.getSerializedString(connectionTypeAdapter)
                        || attributeDAO.isHidden != isHidden
                        || arePropertiesDirty()
            }

        fun makeFrontalChangesToDao(): OTAttributeDAO {
            val dao = OTAttributeDAO()
            dao._id = attributeDAO._id
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

            dao.serializedConnection = connectionObservable.value?.datum?.getSerializedString(connectionTypeAdapter)

            return dao
        }

        fun applyDaoChangesToFront(editedDao: OTAttributeDAO) {
            name = editedDao.name

            isRequired = editedDao.isRequired

            isEditable.onNextIfDifferAndNotNull(editedDao.isEditingAllowed())
            isRemovable.onNextIfDifferAndNotNull(editedDao.isRemovalAllowed())
            isVisibilityEditable.onNextIfDifferAndNotNull(editedDao.isVisibilityToggleAllowed())

            defaultValuePolicy = editedDao.fallbackValuePolicy
            defaultValuePreset = editedDao.fallbackPresetSerializedValue
            isHidden = editedDao.isHidden

            println("serialized connection of dao: ${editedDao.serializedConnection}")
            editedDao.serializedConnection?.let {
                val connection = connectionTypeAdapter.fromJson(it)
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

            val helper = attributeManager.get(editedDao.type)
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
                attributeDAO.serializedConnection = connectionObservable.value?.datum?.getSerializedString(connectionTypeAdapter)
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