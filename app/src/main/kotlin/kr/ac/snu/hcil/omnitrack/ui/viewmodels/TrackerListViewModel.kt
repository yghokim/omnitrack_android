package kr.ac.snu.hcil.omnitrack.ui.viewmodels

import android.support.v7.util.DiffUtil
import io.realm.*
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.database.local.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import rx.Observable
import rx.Subscription
import rx.subjects.BehaviorSubject
import rx.subscriptions.CompositeSubscription

/**
 * Created by younghokim on 2017-05-30.
 */

class TrackerListViewModel : UserAttachedViewModel(), OrderedRealmCollectionChangeListener<RealmResults<OTTrackerDAO>> {

    private val realm = OTApplication.app.databaseManager.getRealmInstance()
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
                        newDaos.map { TrackerInformationViewModel(it, realm) }
                )

                trackerViewModelListSubject.onNext(
                        currentTrackerViewModelList
                )
            }
        }
    }

    override fun onUserAttached(newUserId: String) {
        super.onUserAttached(newUserId)
        trackersRealmResults?.removeAllChangeListeners()
        clearTrackerViewModelList()
        val trackerQueryResults = OTApplication.app.databaseManager.findTrackersOfUser(newUserId, realm)
        trackersRealmResults = trackerQueryResults

        trackerQueryResults.addChangeListener(this)
    }

    override fun onDispose() {
        super.onDispose()
        trackersRealmResults?.removeAllChangeListeners()
        clearTrackerViewModelList()
        realm.close()
    }

    private fun clearTrackerViewModelList() {
        currentTrackerViewModelList.forEach {
            it.unregister()
        }
        currentTrackerViewModelList.clear()
        trackerViewModelListSubject.onNext(emptyList())
    }

    class TrackerInformationViewModel(val trackerDao: OTTrackerDAO, val realm: Realm) : RealmChangeListener<OTTrackerDAO> {

        val totalItemCount: BehaviorSubject<Long> = BehaviorSubject.create()

        val todayCount: BehaviorSubject<Long> = BehaviorSubject.create()

        val lastLoggingTime: BehaviorSubject<Long> = BehaviorSubject.create()

        val trackerName: BehaviorSubject<String> = BehaviorSubject.create()
        val trackerColor: BehaviorSubject<Int> = BehaviorSubject.create()

        val activeNotificationCount: BehaviorSubject<Int> = BehaviorSubject.create()

        val trackerEditable: BehaviorSubject<Boolean> = BehaviorSubject.create()

        val trackerItemsResult: RealmResults<OTItemDAO> = OTApplication.app.databaseManager.makeItemsQuery(trackerDao.objectId, null, null, realm).findAllAsync()
        val todayItemsResult: RealmResults<OTItemDAO> = OTApplication.app.databaseManager.makeItemsQueryOfToday(trackerDao.objectId, realm).findAllAsync()

        //private val countTracer: ItemCountTracer = ItemCountTracer(tracker)

        private val subscriptions = CompositeSubscription()

        private val reminderSubscriptionDict = android.support.v4.util.ArrayMap<String, CompositeSubscription>()

        init {

            trackerItemsResult.addChangeListener { snapshot, changeSet ->
                if (changeSet == null) {
                    //first
                }

                snapshot.count().toLong().let {
                    if (totalItemCount.value != it) {
                        totalItemCount.onNext(it)
                    }
                }

                snapshot.max(RealmDatabaseManager.FIELD_TIMESTAMP_LONG)?.toLong()?.let {
                    if (lastLoggingTime.value != it) {
                        lastLoggingTime.onNext(it)
                    }
                } ?: lastLoggingTime.onNext(null)
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

        private fun addSubscriptionToTrigger(trigger: OTTrigger, subscription: Subscription) {
            if (!reminderSubscriptionDict.contains(trigger.objectId)) {
                reminderSubscriptionDict[trigger.objectId] = CompositeSubscription()
            }
            reminderSubscriptionDict[trigger.objectId]?.add(subscription)
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

            this.trackerItemsResult.removeAllChangeListeners()
            this.todayItemsResult.removeAllChangeListeners()

            subscriptions.clear()
            reminderSubscriptionDict.forEach { entry -> entry.value?.clear() }
            reminderSubscriptionDict.clear()

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