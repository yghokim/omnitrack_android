package kr.ac.snu.hcil.omnitrack.ui.pages.home

import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.realm.*
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.UserAttachedViewModel
import kr.ac.snu.hcil.omnitrack.utils.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.utils.move

/**
 * Created by younghokim on 2017. 10. 31..
 */
class OrderedTrackerListViewModel: UserAttachedViewModel(), OrderedRealmCollectionChangeListener<RealmResults<OTTrackerDAO>> {

    private val realm: Realm = OTApp.instance.databaseManager.getRealmInstance()

    private var trackerQueryResults: RealmResults<OTTrackerDAO>? = null

    private val currentOrderedTrackerViewModels = ArrayList<OrderedTrackerViewModel>()

    val orderedTrackerViewModels = BehaviorSubject.create<List<OrderedTrackerViewModel>>()

    val onTrackerInfoChanged = PublishSubject.create<String>()

    override fun onUserAttached(newUserId: String) {
        super.onUserAttached(newUserId)
        trackerQueryResults = OTApp.instance.databaseManager.makeTrackersOfUserQuery(newUserId, realm)
                .findAllSortedAsync("position", Sort.ASCENDING)

        trackerQueryResults?.addChangeListener(this)
    }

    override fun onChange(snapshot: RealmResults<OTTrackerDAO>, changeSet: OrderedCollectionChangeSet?) {
        if(changeSet == null)
        {
            //initial
            currentOrderedTrackerViewModels.clear()
            currentOrderedTrackerViewModels.addAll(snapshot.map{OrderedTrackerViewModel(it)})
            orderedTrackerViewModels.onNext(currentOrderedTrackerViewModels)
        }
        else{
            currentOrderedTrackerViewModels.removeAll(changeSet.deletions.map { currentOrderedTrackerViewModels[it] })
            changeSet.insertions.forEach {
                currentOrderedTrackerViewModels.add(it, OrderedTrackerViewModel(snapshot[it]!!))
            }

            changeSet.changes.forEach {
                onTrackerInfoChanged.onNext(currentOrderedTrackerViewModels[it].objectId!!)
            }
        }
    }

    override fun onUserDisposed() {
        super.onUserDisposed()
        trackerQueryResults?.removeAllChangeListeners()
    }

    override fun onCleared() {
        super.onCleared()
        realm.close()
    }

    fun moveTracker(from: Int, to: Int){
        currentOrderedTrackerViewModels.move(from, to)
        orderedTrackerViewModels.onNext(currentOrderedTrackerViewModels)
    }

    fun applyOrders(){
        realm.executeTransaction {
            currentOrderedTrackerViewModels.forEachIndexed { index, viewModel ->
                viewModel.dao.position = index
            }
        }
    }

    class OrderedTrackerViewModel(internal val dao: OTTrackerDAO) : IReadonlyObjectId {
        override val objectId: String?
            get() = dao.objectId
        val name: String get() = dao.name
        val color: Int get() = dao.color
    }
}