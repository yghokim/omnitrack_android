package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.support.v7.util.DiffUtil
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.realm.*
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.UserAttachedViewModel
import kr.ac.snu.hcil.omnitrack.utils.DefaultNameGenerator
import kr.ac.snu.hcil.omnitrack.utils.Nullable

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

                //deal with deletions
                val removes = changeSet.deletions.map { i -> currentTrackerViewModelList[i] }
                removes.forEach { it.unregister() }
                currentTrackerViewModelList.removeAll(removes)

                //deal with additions
                val newDaos = changeSet.insertions.map { i -> snapshot[i] }
                currentTrackerViewModelList.addAll(
                        newDaos.mapNotNull { it?.let { TrackerInformationViewModel(it, realm) } }
                )

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
        val trackerQueryResults = OTApp.instance.databaseManager.findTrackersOfUser(newUserId, realm)
        trackersRealmResults = trackerQueryResults

        trackerQueryResults.addChangeListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        trackersRealmResults?.removeAllChangeListeners()
        realm.close()
    }

    override fun onDispose() {
        super.onDispose()
        clearTrackerViewModelList()
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
                OTApp.instance.databaseManager.removeTracker(model.trackerDao)
            }
        }
    }

    class TrackerInformationViewModel(val trackerDao: OTTrackerDAO, val realm: Realm) : RealmChangeListener<OTTrackerDAO> {

        val validationResult = BehaviorSubject.create<Pair<Boolean, List<CharSequence>>>()

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

        val activeNotificationCount: BehaviorSubject<Int> = BehaviorSubject.create()

        val trackerEditable: BehaviorSubject<Boolean> = BehaviorSubject.create()

        val attributesResult: RealmResults<OTAttributeDAO> = OTApp.instance.databaseManager.getAttributeListQuery(trackerDao.objectId!!, realm).findAllAsync()
        val trackerItemsResult: RealmResults<OTItemDAO> = OTApp.instance.databaseManager.makeItemsQuery(trackerDao.objectId, null, null, realm).findAllAsync()
        val todayItemsResult: RealmResults<OTItemDAO> = OTApp.instance.databaseManager.makeItemsQueryOfToday(trackerDao.objectId, realm).findAllAsync()

        //private val countTracer: ItemCountTracer = ItemCountTracer(tracker)

        private val subscriptions = CompositeDisposable()

        init {

            attributesResult.addChangeListener { snapshot, changeSet ->
                if (changeSet == null) {
                    //first
                }

                var valid = true
                val validationMessages = ArrayList<CharSequence>()

                snapshot.forEach {
                    if (it.getParsedConnection()?.isAvailableToRequestValue(validationMessages) == false) {
                        valid = false
                    }
                }

                validationResult.onNext(Pair(valid, validationMessages))
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

    class TrackerViewModelListDiffUtilCallback(val oldList: List<TrackerInformationViewModel>, val newList: List<TrackerInformationViewModel>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].trackerDao.objectId == newList[newItemPosition].trackerDao.objectId
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