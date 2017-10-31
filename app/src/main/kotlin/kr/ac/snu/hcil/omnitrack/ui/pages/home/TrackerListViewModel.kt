package kr.ac.snu.hcil.omnitrack.ui.pages.home

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.realm.*
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.local.*
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.UserAttachedViewModel
import kr.ac.snu.hcil.omnitrack.utils.DefaultNameGenerator
import kr.ac.snu.hcil.omnitrack.utils.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.onNextIfDifferAndNotNull
import java.util.*

/**
 * Created by younghokim on 2017-05-30.
 */

class TrackerListViewModel : UserAttachedViewModel(), OrderedRealmCollectionChangeListener<RealmResults<OTTrackerDAO>> {

    private val realm = OTApp.instance.databaseManager.getRealmInstance()
    private var trackersRealmResults: RealmResults<OTTrackerDAO>? = null

    private var trackerViewModelListSubject: BehaviorSubject<List<TrackerInformationViewModel>> = BehaviorSubject.create()

    private val currentTrackerViewModelList = ArrayList<TrackerInformationViewModel>()

    val trackerViewModels: Observable<List<TrackerInformationViewModel>>
        get() = trackerViewModelListSubject

    private val subscriptions = CompositeDisposable()

    override fun onChange(snapshot: RealmResults<OTTrackerDAO>, changeSet: OrderedCollectionChangeSet?) {
        if (snapshot.isLoaded && snapshot.isValid) {
            if (changeSet == null) {
                println("Viewmodel first time limit")
                //first time emit
                clearTrackerViewModelList()
                val viewModels = snapshot.map { TrackerInformationViewModel(it, realm) }
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
                    currentTrackerViewModelList.add(index, TrackerInformationViewModel(snapshot[index]!!, realm))
                }

                trackerViewModelListSubject.onNext(
                        currentTrackerViewModelList
                )
            }
        }
    }

    fun generateNewTrackerName(): String {
        return DefaultNameGenerator.generateName(OTApp.getString(R.string.msg_new_tracker_prefix), currentTrackerViewModelList.map { it.trackerName.value }, false)
    }

    override fun onUserAttached(newUserId: String) {
        super.onUserAttached(newUserId)
        trackersRealmResults?.removeAllChangeListeners()
        clearTrackerViewModelList()
        trackersRealmResults = OTApp.instance.databaseManager.makeTrackersOfUserQuery(newUserId, realm).findAllSortedAsync("position", Sort.ASCENDING)
        trackersRealmResults?.addChangeListener(this)

        subscriptions.add(OTApp.instance.databaseManager.makeShortcutPanelRefreshObservable(newUserId, realm).subscribe())
    }

    override fun onCleared() {
        super.onCleared()
        trackersRealmResults?.removeAllChangeListeners()
        realm.close()
    }

    override fun onUserDisposed() {
        super.onUserDisposed()
        clearTrackerViewModelList()
        subscriptions.clear()
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
                OTApp.instance.databaseManager.removeTracker(model.trackerDao, false, realm)
            }
        }
    }

    class TrackerInformationViewModel(val trackerDao: OTTrackerDAO, val realm: Realm) : IReadonlyObjectId, RealmChangeListener<OTTrackerDAO> {
        override val objectId: String?
            get() = trackerDao.objectId

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

        val activeNotificationCount: BehaviorSubject<Int> = BehaviorSubject.createDefault(0)

        val trackerEditable: BehaviorSubject<Boolean> = BehaviorSubject.create()

        val attributesResult: RealmResults<OTAttributeDAO> = OTApp.instance.databaseManager.getAttributeListQuery(trackerDao.objectId!!, realm).findAllAsync()
        val trackerItemsResult: RealmResults<OTItemDAO> = OTApp.instance.databaseManager.makeItemsQuery(trackerDao.objectId, null, null, realm).findAllAsync()
        val todayItemsResult: RealmResults<OTItemDAO> = OTApp.instance.databaseManager.makeItemsQueryOfToday(trackerDao.objectId, realm).findAllAsync()

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
                                if (validationResult.value.first != true) {
                                    validationResult.onNext(Pair<Boolean, List<CharSequence>?>(true, null))
                                }
                            } else {
                                //collect invalidation messages
                                val messages = attributesResult.filter { currentAttributeValidationResultDict[it.localId]?.first == false }
                                        .mapNotNull { currentAttributeValidationResultDict[it.localId]?.second }

                                if (validationResult.value.first != false) {
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

                lastLoggingTime = snapshot.max(RealmDatabaseManager.FIELD_TIMESTAMP_LONG)?.toLong()
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
            if (trackerColor.value != snapshot.color) {
                trackerColor.onNext(snapshot.color)
            }

            if (trackerName.value != snapshot.name) {
                trackerName.onNext(snapshot.name)
            }

            if (trackerEditable.value != snapshot.isEditable) {
                trackerEditable.onNext(snapshot.isEditable)
            }

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