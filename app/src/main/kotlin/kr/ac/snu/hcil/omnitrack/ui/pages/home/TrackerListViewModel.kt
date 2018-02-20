package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.app.Application
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.realm.*
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.CreationFlagsHelper
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.research.OTExperimentDAO
import kr.ac.snu.hcil.omnitrack.core.di.configured.Research
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.UserAttachedViewModel
import kr.ac.snu.hcil.omnitrack.utils.DefaultNameGenerator
import kr.ac.snu.hcil.omnitrack.utils.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.onNextIfDifferAndNotNull
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

    @field:[Inject Research]
    lateinit var researchRealmFactory: Factory<Realm>

    private var trackersRealmResults: RealmResults<OTTrackerDAO>? = null

    private var trackerViewModelListSubject: BehaviorSubject<List<TrackerInformationViewModel>> = BehaviorSubject.create()

    private val currentTrackerViewModelList = ArrayList<TrackerInformationViewModel>()

    private lateinit var researchRealm: Realm

    val trackerViewModels: Observable<List<TrackerInformationViewModel>>
        get() = trackerViewModelListSubject

    override fun onInject(configuredContext: ConfiguredContext) {
        configuredContext.configuredAppComponent.inject(this)
        researchRealm = researchRealmFactory.get()
    }

    override fun onChange(snapshot: RealmResults<OTTrackerDAO>, changeSet: OrderedCollectionChangeSet?) {
        if (snapshot.isLoaded && snapshot.isValid) {
            if (changeSet == null) {
                println("Viewmodel first time limit")
                //first time emit
                clearTrackerViewModelList()
                val viewModels = snapshot.map { TrackerInformationViewModel(it, realm, researchRealm, dbManager.get()) }
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
                    currentTrackerViewModelList.add(index, TrackerInformationViewModel(snapshot[index]!!, realm, researchRealm, dbManager.get()))
                }

                trackerViewModelListSubject.onNext(
                        currentTrackerViewModelList
                )
            }
        }
    }

    fun getPermissionsRequiredForFields(): Set<String> {
        val set = HashSet<String>()
        for (tracker in currentTrackerViewModelList) {
            tracker.trackerDao.attributes
                    .mapNotNull { it.getHelper(configuredContext).getRequiredPermissions(it) }
                    .forEach { set.addAll(it) }
        }
        return set
    }

    fun generateNewTrackerName(): String {
        return DefaultNameGenerator.generateName(OTApp.getString(R.string.msg_new_tracker_prefix), currentTrackerViewModelList.map { it.trackerName.value }, false)
    }

    override fun onUserAttached(newUserId: String) {
        super.onUserAttached(newUserId)
        trackersRealmResults?.removeAllChangeListeners()
        clearTrackerViewModelList()
        trackersRealmResults = dbManager.get().makeTrackersOfUserQuery(newUserId, realm).sort(arrayOf("position", BackendDbManager.FIELD_USER_CREATED_AT), arrayOf(Sort.ASCENDING, Sort.DESCENDING)).findAllAsync()
        trackersRealmResults?.addChangeListener(this)

        shortcutPanelManager.get().registerShortcutRefreshSubscription(newUserId, TAG)
    }

    override fun onCleared() {
        trackersRealmResults?.removeAllChangeListeners()
        super.onCleared()
    }

    override fun onUserDisposed() {
        super.onUserDisposed()
        researchRealm.close()
        clearTrackerViewModelList()
        shortcutPanelManager.get().unregisterShortcutRefreshSubscription(TAG)
    }

    private fun clearTrackerViewModelList() {
        currentTrackerViewModelList.forEach {
            it.unregister()
        }
        currentTrackerViewModelList.clear()
        trackerViewModelListSubject.onNext(emptyList())
    }

    fun removeTracker(model: TrackerInformationViewModel) {
        if (!realm.isInTransaction) {
            realm.executeTransaction {
                dbManager.get().removeTracker(model.trackerDao, false, realm)
            }
        }
        syncManager.get().registerSyncQueue(ESyncDataType.TRACKER, SyncDirection.UPLOAD)
    }

    class TrackerInformationViewModel(val trackerDao: OTTrackerDAO, val realm: Realm, val researchRealm: Realm, dbManager: BackendDbManager) : IReadonlyObjectId, RealmChangeListener<OTTrackerDAO> {
        override val objectId: String?
            get() = _objectId

        private var _objectId: String? = trackerDao.objectId

        val validationResult = BehaviorSubject.createDefault<Pair<Boolean, List<CharSequence>?>>(Pair(true, null))

        val totalItemCount: BehaviorSubject<Long> = BehaviorSubject.create()

        val todayCount: BehaviorSubject<Long> = BehaviorSubject.create()

        val lastLoggingTimeObservable: BehaviorSubject<Nullable<Long>> = BehaviorSubject.createDefault(Nullable())
        var lastLoggingTime: Long?
            get() = lastLoggingTimeObservable.value.datum
            private set(value) {
                if (lastLoggingTimeObservable.value.datum != value) {
                    lastLoggingTimeObservable.onNext(Nullable(value))
                }
            }

        val trackerName: BehaviorSubject<String> = BehaviorSubject.create()
        val trackerColor: BehaviorSubject<Int> = BehaviorSubject.create()

        val isForExperiment: BehaviorSubject<Boolean> = BehaviorSubject.create()
        val experimentName: BehaviorSubject<Nullable<String>> = BehaviorSubject.create()

        val activeNotificationCount: BehaviorSubject<Int> = BehaviorSubject.createDefault(0)

        val trackerRemovable: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(true)
        val trackerEditable: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(true)


        val isBookmarked: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)

        val attributesResult: RealmResults<OTAttributeDAO> = trackerDao.makeAttributesQuery(false, false).findAllAsync()
        val trackerItemsResult: RealmResults<OTItemDAO> = dbManager.makeItemsQuery(trackerDao.objectId, null, null, realm).findAllAsync()
        val todayItemsResult: RealmResults<OTItemDAO> = dbManager.makeItemsQueryOfTheDay(trackerDao.objectId, realm).findAllAsync()

        private val currentAttributeValidationResultDict = Hashtable<String, Pair<Boolean, CharSequence?>>()

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
                if (changeSet == null) {
                    //first
                }

                //validation
                currentAttributeValidationResultDict.clear()
                subscriptions.add(
                        Flowable.merge(
                                snapshot.map {
                                    (it.getParsedConnection()?.makeValidationStateObservable()
                                            ?: Flowable.just(Pair<Boolean, CharSequence?>(true, null))).map { validationResult ->
                                        Pair(it.localId, validationResult)
                                    }
                                }
                        ).doOnNext { (localId, validationResult) ->
                            currentAttributeValidationResultDict[localId] = validationResult
                        }.subscribe {
                            if (attributesResult.find { currentAttributeValidationResultDict[it.localId]?.first == false } == null) {
                                if (!validationResult.value.first) {
                                    validationResult.onNext(Pair<Boolean, List<CharSequence>?>(true, null))
                                }
                            } else {
                                //collect invalidation messages
                                val messages = attributesResult.filter { currentAttributeValidationResultDict[it.localId]?.first == false }
                                        .mapNotNull { currentAttributeValidationResultDict[it.localId]?.second }

                                if (validationResult.value.first) {
                                    validationResult.onNext(Pair(false, messages))
                                }
                            }
                    }
                )
            }

            trackerItemsResult.addChangeListener { snapshot, changeSet ->
                if (changeSet == null) {
                    //first
                }

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

            CreationFlagsHelper.getExperimentId(trackerDao.getParsedCreationFlags())?.let { experimentId ->
                println("Observe the name of the experiment : ${experimentId}")
                subscriptions.add(
                        researchRealm.where(OTExperimentDAO::class.java)
                                .equalTo("id", experimentId)
                                .findFirstAsync().asFlowable<OTExperimentDAO>()
                                .filter {
                                    it.isValid && it.isLoaded
                                }
                                .subscribe { experimentInfo ->
                                    this.experimentName.onNextIfDifferAndNotNull(Nullable(experimentInfo.name))
                                }
                )
            }

            trackerDao.addChangeListener(this)
            updateValues(trackerDao)
        }

        private fun updateValues(snapshot: OTTrackerDAO) {
            trackerColor.onNextIfDifferAndNotNull(snapshot.color)
            trackerName.onNextIfDifferAndNotNull(snapshot.name)
            isBookmarked.onNextIfDifferAndNotNull(snapshot.isBookmarked)
            isForExperiment.onNextIfDifferAndNotNull(CreationFlagsHelper.isForExperiment(snapshot.getParsedCreationFlags()))

            trackerEditable.onNextIfDifferAndNotNull(!snapshot.isEditingLocked())
            trackerRemovable.onNextIfDifferAndNotNull(!snapshot.isDeletionLocked())

        }

        override fun onChange(snapshot: OTTrackerDAO) {
            println("tracker information viewmodel onChange")
            println(snapshot.name)
            println(snapshot.color)
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