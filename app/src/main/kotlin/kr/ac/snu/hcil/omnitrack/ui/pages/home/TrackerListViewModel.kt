package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.Lazy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.realm.*
import kr.ac.snu.hcil.android.common.DefaultNameGenerator
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.onNextIfDifferAndNotNull
import kr.ac.snu.hcil.android.common.view.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.flags.CreationFlagsHelper
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSynchronizationCommands
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.UserAttachedViewModel
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import java.util.*
import javax.inject.Inject

/**
 * Created by younghokim on 2017-05-30.
 */

class TrackerListViewModel(app: Application) : UserAttachedViewModel(app), OrderedRealmCollectionChangeListener<RealmResults<OTTrackerDAO>> {

    companion object {
        const val TAG = "TrackerListViewModel"
    }

    @Inject
    lateinit var syncManager: Lazy<OTSyncManager>

    @Inject
    lateinit var shortcutPanelManager: Lazy<OTShortcutPanelManager>

    private var trackersRealmResults: RealmResults<OTTrackerDAO>? = null

    private var trackerViewModelListSubject: BehaviorSubject<List<TrackerInformationViewModel>> = BehaviorSubject.create()

    private val currentTrackerViewModelList = ArrayList<TrackerInformationViewModel>()

    private val requiredPermissionsSubject: BehaviorSubject<Array<String>> = BehaviorSubject.create()

    val trackerViewModels: Observable<List<TrackerInformationViewModel>>
        get() = trackerViewModelListSubject

    val requiredPermissions: Observable<Array<String>>
        get() = requiredPermissionsSubject

    private val syncIntentFilter: IntentFilter by lazy {
        IntentFilter(OTSynchronizationCommands.BROADCAST_ACTION_SYNCHRONIZATION_FINISHED)
    }
    private val syncBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == OTSynchronizationCommands.BROADCAST_ACTION_SYNCHRONIZATION_FINISHED) {
                val entityTypes = intent.getIntArrayExtra(OTSynchronizationCommands.BROADCAST_EXTRA_ENTITY_TYPES).map { ESyncDataType.values()[it] }
                println("synchronization finished: ${entityTypes.joinToString(", ")}")
            }
        }
    }

    override fun onInject(app: OTAndroidApp) {
        getApplication<OTApp>().applicationComponent.inject(this)
    }

    override fun onChange(snapshot: RealmResults<OTTrackerDAO>, changeSet: OrderedCollectionChangeSet) {
        if (snapshot.isLoaded && snapshot.isValid) {
            if (changeSet.state == OrderedCollectionChangeSet.State.INITIAL) {
                //first time emit
                clearTrackerViewModelList()
                val viewModels = snapshot.map { TrackerInformationViewModel(it, realm, dbManager.get(), getApplication()) }
                currentTrackerViewModelList.addAll(viewModels)

                trackerViewModelListSubject.onNext(
                        currentTrackerViewModelList
                )
            } else {
                println("tracker list changed")
                //deal with deletions
                val removes = changeSet.deletions.map { i -> currentTrackerViewModelList[i] }
                removes.forEach { it.unregister() }
                currentTrackerViewModelList.removeAll(removes)

                //deal with additions
                changeSet.insertions.forEach { index ->
                    currentTrackerViewModelList.add(index, TrackerInformationViewModel(snapshot[index]!!, realm, dbManager.get(), getApplication()))
                }

                trackerViewModelListSubject.onNext(
                        currentTrackerViewModelList
                )
            }
        }
    }

    private fun makeTrackerPermissions(): Flowable<Set<String>> {
        return realm.where(OTTrackerDAO::class.java)
                .equalTo(BackendDbManager.FIELD_REMOVED_BOOLEAN, false)
                .equalTo(BackendDbManager.FIELD_USER_ID, userId)
                .findAllAsync().asFlowable().filter { it.isValid == true && it.isLoaded == true }
                .map { trackers ->
                    val set = HashSet<String>()
                    for (tracker in trackers) {
                        tracker.attributes
                                .mapNotNull { it.getHelper(getApplication()).getRequiredPermissions(it) }
                                .forEach { set.addAll(it) }
                    }
                    set
                }
    }

    fun generateNewTrackerName(): String {
        return DefaultNameGenerator.generateName(getApplication<OTApp>().getString(R.string.msg_new_tracker_prefix), currentTrackerViewModelList.map {
            it.trackerName.value ?: ""
        }, false)
    }

    override fun onUserAttached(newUserId: String) {
        super.onUserAttached(newUserId)
        trackersRealmResults?.removeAllChangeListeners()
        clearTrackerViewModelList()
        trackersRealmResults = dbManager.get().makeTrackersOfUserVisibleQuery(newUserId, realm).findAllAsync()
        trackersRealmResults?.addChangeListener(this)

        shortcutPanelManager.get().registerShortcutRefreshSubscription(newUserId, TAG)

        LocalBroadcastManager.getInstance(getApplication()).registerReceiver(syncBroadcastReceiver, syncIntentFilter)

        this.subscriptions.add(
                makeTrackerPermissions().subscribe { permissions ->
                    this.requiredPermissionsSubject.onNext(permissions.toTypedArray())
                }
        )
    }

    override fun onCleared() {
        trackersRealmResults?.removeAllChangeListeners()
        super.onCleared()
    }

    override fun onUserDisposed() {
        super.onUserDisposed()
        clearTrackerViewModelList()
        shortcutPanelManager.get().unregisterShortcutRefreshSubscription(TAG)
        LocalBroadcastManager.getInstance(getApplication()).unregisterReceiver(syncBroadcastReceiver)
    }

    private fun clearTrackerViewModelList() {
        currentTrackerViewModelList.forEach {
            it.unregister()
        }
        currentTrackerViewModelList.clear()
        trackerViewModelListSubject.onNext(emptyList())
    }

    fun removeTracker(model: TrackerInformationViewModel) {
        var triggerRemoved = false
        realm.executeTransactionIfNotIn {
            dbManager.get().removeTracker(model.trackerDao, false, realm)

            //remove associated reminders
            model.trackerDao.liveTriggersQuery?.equalTo("actionType", OTTriggerDAO.ACTION_TYPE_REMIND)?.findAll()?.let { reminders ->
                if (reminders.size > 0) triggerRemoved = true
                reminders.forEach {
                    dbManager.get().removeTrigger(it, false, realm)
                }
            }
        }
        syncManager.get().registerSyncQueue(ESyncDataType.TRACKER, SyncDirection.UPLOAD, ignoreDirtyFlags = false)
        if (triggerRemoved) {
            syncManager.get().registerSyncQueue(ESyncDataType.TRIGGER, SyncDirection.UPLOAD, ignoreDirtyFlags = false)
        }
    }

    class TrackerInformationViewModel(val trackerDao: OTTrackerDAO, val realm: Realm, dbManager: BackendDbManager, val context: Context) : IReadonlyObjectId, RealmChangeListener<OTTrackerDAO> {
        override val _id: String?
            get() = _objectId

        private var _objectId: String? = trackerDao._id

        val validationResult = BehaviorSubject.createDefault<Pair<Boolean, List<CharSequence>?>>(Pair(true, null))

        val totalItemCount: BehaviorSubject<Long> = BehaviorSubject.create()

        val todayCount: BehaviorSubject<Long> = BehaviorSubject.create()

        val lastLoggingTimeObservable: BehaviorSubject<Nullable<Long>> = BehaviorSubject.createDefault(Nullable())
        var lastLoggingTime: Long?
            get() = lastLoggingTimeObservable.value?.datum
            private set(value) {
                lastLoggingTimeObservable.onNextIfDifferAndNotNull(Nullable(value))
            }

        val trackerName: BehaviorSubject<String> = BehaviorSubject.create()
        val trackerColor: BehaviorSubject<Int> = BehaviorSubject.create()

        val isForExperiment: BehaviorSubject<Boolean> = BehaviorSubject.create()
        val experimentName: BehaviorSubject<Nullable<String>> = BehaviorSubject.create()

        val activeNotificationCount: BehaviorSubject<Int> = BehaviorSubject.createDefault(0)

        val trackerRemovable: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(true)
        val trackerEditable: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(true)
        val isVisualizationAllowed: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(true)
        val isItemListAllowed: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(true)


        val isBookmarked: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)

        val attributesResult: RealmResults<OTAttributeDAO> = trackerDao.makeAttributesQuery(false, false).findAllAsync()
        val trackerItemsResult: RealmResults<OTItemDAO> = dbManager.makeItemsQuery(trackerDao._id, null, null, realm).findAllAsync()
        val todayItemsResult: RealmResults<OTItemDAO> = dbManager.makeItemsQueryOfTheDay(trackerDao._id, realm).findAllAsync()

        private val currentAttributeValidationResultDict = Hashtable<String, Pair<Boolean, List<CharSequence>?>>()

        private val subscriptions = CompositeDisposable()

        init {
            trackerDao.liveTriggersQuery?.let {
                subscriptions.add(
                        it.equalTo("isOn", true).equalTo("actionType", OTTriggerDAO.ACTION_TYPE_REMIND)
                                .findAllAsync().asFlowable().subscribe { results ->
                            activeNotificationCount.onNextIfDifferAndNotNull(results.size)
                        }
                )
            }

            attributesResult.addChangeListener { snapshot, changeSet ->

                //validation
                currentAttributeValidationResultDict.clear()
                subscriptions.add(
                        Observable.merge(
                                snapshot.map {
                                    (it.getParsedConnection(context)?.makeAvailabilityCheckObservable(it)
                                            ?: Observable.just(Pair<Boolean, List<CharSequence>?>(true, null))).map { validationResult ->
                                        Pair(it.localId, validationResult)
                                    }
                                }
                        ).doOnNext { (localId, validationResult) ->
                            currentAttributeValidationResultDict[localId] = validationResult
                        }.subscribe {
                            if (attributesResult.find { currentAttributeValidationResultDict[it.localId]?.first == false } == null) {
                                if (!(validationResult.value?.first != false)) {
                                    validationResult.onNext(Pair<Boolean, List<CharSequence>?>(true, null))
                                }
                            } else {
                                //collect invalidation messages
                                val messages = attributesResult.filter { currentAttributeValidationResultDict[it.localId]?.first == false }
                                        .mapNotNull { currentAttributeValidationResultDict[it.localId]?.second }
                                        .flatten()

                                if (validationResult.value?.first != false) {
                                    validationResult.onNext(Pair(false, messages))
                                }
                            }
                    }
                )
            }

            trackerItemsResult.addChangeListener { snapshot, changeSet ->
                snapshot.count().toLong().let {
                    if (totalItemCount.value != it) {
                        totalItemCount.onNext(it)
                    }
                }

                lastLoggingTime = snapshot.max(BackendDbManager.FIELD_TIMESTAMP_LONG)?.toLong()
            }

            todayItemsResult.addChangeListener { snapshot, changeSet ->
                val count = snapshot.count().toLong()
                if (todayCount.value != count) {
                    todayCount.onNext(count)
                }
            }

            trackerDao.addChangeListener(this)
            updateValues(trackerDao)
        }

        private fun updateValues(snapshot: OTTrackerDAO) {

            trackerDao.clearCreationFlagsCache()
            trackerColor.onNextIfDifferAndNotNull(snapshot.color)
            trackerName.onNextIfDifferAndNotNull(snapshot.name)
            isBookmarked.onNextIfDifferAndNotNull(snapshot.isBookmarked)
            isForExperiment.onNextIfDifferAndNotNull(CreationFlagsHelper.isForExperiment(snapshot.getParsedCreationFlags()))

            trackerEditable.onNextIfDifferAndNotNull(snapshot.isEditingAllowed())
            trackerRemovable.onNextIfDifferAndNotNull(snapshot.isRemovalAllowed())
            isItemListAllowed.onNextIfDifferAndNotNull(snapshot.isItemListAccessAllowed())
            isVisualizationAllowed.onNextIfDifferAndNotNull(snapshot.isVisualizationAccessAllowed())
        }

        override fun onChange(snapshot: OTTrackerDAO) {
            if (snapshot.isValid && snapshot.isLoaded) {
                updateValues(snapshot)
            }
        }

        fun unregister() {
            trackerDao.removeChangeListener(this)

            this.attributesResult.removeAllChangeListeners()
            this.trackerItemsResult.removeAllChangeListeners()
            this.todayItemsResult.removeAllChangeListeners()

            subscriptions.clear()
        }
    }
}