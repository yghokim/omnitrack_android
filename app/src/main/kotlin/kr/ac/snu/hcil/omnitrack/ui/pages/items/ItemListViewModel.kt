package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Application
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.RealmResults
import io.realm.Sort
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.onNextIfDifferAndNotNull
import kr.ac.snu.hcil.android.common.view.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.ItemLoggingSource
import kr.ac.snu.hcil.omnitrack.core.fields.logics.ItemComparator
import kr.ac.snu.hcil.omnitrack.core.fields.logics.TimestampSorter
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.flags.F
import kr.ac.snu.hcil.omnitrack.core.flags.LockFlagLevel
import kr.ac.snu.hcil.omnitrack.core.flags.LockedPropertiesHelper
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.RealmViewModel
import javax.inject.Inject

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
/**
 * Created by Young-Ho on 10/12/2017.
 */
class ItemListViewModel(app: Application) : RealmViewModel(app), OrderedRealmCollectionChangeListener<RealmResults<OTItemDAO>> {

    @Inject
    protected lateinit var syncManager: OTSyncManager

    private var currentItemQueryResults: RealmResults<OTItemDAO>? = null
    lateinit var trackerDao: OTTrackerDAO
        private set

    private lateinit var managedTrackerDao: OTTrackerDAO
    private lateinit var managedFieldList: RealmResults<OTFieldDAO>

    val trackerId: String get() = trackerDao._id!!

    val trackerNameObservable = BehaviorSubject.createDefault<String>("")
    var trackerName: String
        get() = trackerNameObservable.value ?: ""
        private set(value) {
            if (trackerNameObservable.value != value) {
                trackerNameObservable.onNext(value)
            }
        }

    lateinit var fields: List<OTFieldDAO>
        private set
    //localKey / Type
    val onSchemaChanged = PublishSubject.create<List<OTFieldDAO>>()

    val sorterSetObservable = BehaviorSubject.create<List<ItemComparator>>()
    private val currentSorterSet = ArrayList<ItemComparator>()

    val currentSorterObservable = BehaviorSubject.createDefault<ItemComparator>(TimestampSorter(getApplication()))
    var currentSorter: ItemComparator
        get() = currentSorterObservable.value ?: TimestampSorter(getApplication())
        set(value) {
            currentSorterObservable.onNext(value)
        }

    private val itemsInTimestampDescendingOrder = ArrayList<ItemViewModel>()


    private val itemsSortedList = ArrayList<ItemViewModel>()

    val sortedItemsObservable = BehaviorSubject.create<List<ItemViewModel>>()

    private var _isItemModificationAllowed: Boolean? = null
    val isItemModificationAllowed: Boolean
        get() {
            if (_isItemModificationAllowed == null) {
                _isItemModificationAllowed = LockedPropertiesHelper.flag(LockFlagLevel.Tracker, F.ModifyItems, if (this::trackerDao.isInitialized) {
                    this.trackerDao.getParsedLockedPropertyInfo()
                } else null)
            }
            return _isItemModificationAllowed!!
        }

    private val itemComparerMethod = Comparator<ItemViewModel> { p0, p1 -> currentSorter.compare(p0?.itemDao, p1?.itemDao) }

    val onItemContentChanged = PublishSubject.create<Array<OrderedCollectionChangeSet.Range>>()

    override fun onInject(app: OTAndroidApp) {
        app.applicationComponent.inject(this)
    }

    fun init(trackerId: String) {
        val dao = dbManager.get().getTrackerQueryWithId(trackerId, realm).findFirst()

        if (dao != null) {
            managedTrackerDao = dbManager.get().getTrackerQueryWithId(trackerId, realm).findFirstAsync()
            managedTrackerDao.addChangeListener<OTTrackerDAO> { snapshot ->
                println("tracker db changed in background")
            }
            managedFieldList = dbManager.get().getAttributeListQuery(trackerId, realm).findAllAsync()
            managedFieldList.addChangeListener { snapshot, changeSet ->
                println("tracker field list db changed in background")
            }

            refreshTracker(realm.copyFromRealm(dao))
        }
    }

    private fun refreshTracker(trackerDao: OTTrackerDAO) {
        this._isItemModificationAllowed = null
        this.trackerDao = trackerDao

        this.fields = trackerDao.fields.asSequence().filter { it.isHidden == false && it.isInTrashcan == false }.toList()
        trackerName = trackerDao.name

        currentSorterSet.clear()
        currentSorterSet.add(TimestampSorter(getApplication()))
        fields.forEach {
            currentSorterSet += it.getHelper(getApplication()).getSupportedSorters(it)
        }

        sorterSetObservable.onNext(currentSorterSet)
        onSchemaChanged.onNext(fields)

        currentItemQueryResults?.removeAllChangeListeners()
        currentItemQueryResults = dbManager.get()
                .makeItemsQuery(trackerDao._id, null, null, realm)
                .sort("timestamp", Sort.DESCENDING)
                .findAllAsync()
        currentItemQueryResults?.addChangeListener(this)
    }

    private fun refreshSortedItems() {
        itemsSortedList.clear()
        itemsSortedList.addAll(itemsInTimestampDescendingOrder)
        itemsSortedList.sortWith(itemComparerMethod)

        sortedItemsObservable.onNext(itemsSortedList)
    }

    override fun onChange(snapshot: RealmResults<OTItemDAO>, changeSet: OrderedCollectionChangeSet) {
        if (changeSet.state == OrderedCollectionChangeSet.State.INITIAL) {
            //initial fetch
            itemsInTimestampDescendingOrder.clear()
            itemsInTimestampDescendingOrder.addAll(snapshot.map { ItemViewModel(it) })

            refreshSortedItems()

        } else {
            //delete removed//deal with deletions
            val removes = changeSet.deletions.map { i -> itemsInTimestampDescendingOrder[i] }
            itemsInTimestampDescendingOrder.removeAll(removes)

            //deal with additions
            changeSet.insertions.forEach { i ->
                itemsInTimestampDescendingOrder.add(i, ItemViewModel(snapshot[i]!!))
            }

            refreshSortedItems()

            onItemContentChanged.onNext(changeSet.changeRanges)
        }
    }

    fun removeItem(itemId: String) {
        val viewModel = itemsInTimestampDescendingOrder.find { it._id == itemId }
        if (viewModel != null) {
            realm.executeTransaction {
                dbManager.get().removeItem(viewModel.itemDao, false, realm)
            }
            syncManager.registerSyncQueue(ESyncDataType.ITEM, SyncDirection.UPLOAD, ignoreDirtyFlags = false)
        }
    }

    fun setSorter(itemComparator: ItemComparator) {
        currentSorter = itemComparator
        itemsSortedList.sortWith(itemComparerMethod)
        sortedItemsObservable.onNext(itemsSortedList)
    }

    inner class ItemViewModel(val itemDao: OTItemDAO) : IReadonlyObjectId {
        override val _id: String? get() = _objectId

        private var _objectId: String? = null

        fun getItemValueOf(fieldLocalId: String): Any? = itemDao.getValueOf(fieldLocalId)

        val timestampObservable = BehaviorSubject.create<Long>()
        var timestamp: Long
            get() = timestampObservable.value ?: 0
            private set(value) {
                if (timestampObservable.value != value) {
                    timestampObservable.onNext(value)
                }
            }

        val timezoneObservable = BehaviorSubject.create<Nullable<String>>()
        var timezone: String?
            get() = timezoneObservable.value?.datum
            private set(value) {
                timezoneObservable.onNextIfDifferAndNotNull(Nullable(value))
            }

        val loggingSourceObservable = BehaviorSubject.create<ItemLoggingSource>()
        var loggingSource: ItemLoggingSource
            get() = loggingSourceObservable.value ?: ItemLoggingSource.Unspecified
            set(value) {
                if (loggingSourceObservable.value != value) {
                    loggingSourceObservable.onNext(value)
                }
            }

        val syncFlagObservable = BehaviorSubject.create<Boolean>()
        var isSynchronized: Boolean
            get() = syncFlagObservable.value ?: false
            private set(value) {
                syncFlagObservable.onNextIfDifferAndNotNull(value)
            }

        init {
            _objectId = itemDao._id
            timestamp = itemDao.timestamp
            timezone = itemDao.timezone
            loggingSource = itemDao.loggingSource
            isSynchronized = itemDao.synchronizedAt != null
        }

        fun setValueOf(fieldLocalId: String, serializedValue: String?) {
            realm.executeTransaction {
                itemDao.synchronizedAt = null
                itemDao.setValueOf(fieldLocalId, serializedValue)
            }
        }

        fun save(vararg changedLocalIds: String): Single<Pair<Int, String?>> {

            return dbManager.get().saveItemObservable(itemDao, false, changedLocalIds.toList().toTypedArray(), realm)
                    .doAfterSuccess { (resultCode, _) ->
                        if (resultCode != BackendDbManager.SAVE_RESULT_FAIL) {
                            syncManager.registerSyncQueue(ESyncDataType.ITEM, SyncDirection.UPLOAD, ignoreDirtyFlags = false)
                        }
                    }
        }

    }
}