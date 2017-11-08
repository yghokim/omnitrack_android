package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.app.Application
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.RealmResults
import io.realm.Sort
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.UserAttachedViewModel
import kr.ac.snu.hcil.omnitrack.utils.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.utils.move
import java.util.*
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 10. 31..
 */
class OrderedTrackerListViewModel(app: Application) : UserAttachedViewModel(app), OrderedRealmCollectionChangeListener<RealmResults<OTTrackerDAO>> {

    @Inject
    protected lateinit var syncManager: OTSyncManager

    private var trackerQueryResults: RealmResults<OTTrackerDAO>? = null

    private val currentOrderedTrackerViewModels = ArrayList<OrderedTrackerViewModel>()

    val orderedTrackerViewModels = BehaviorSubject.create<List<OrderedTrackerViewModel>>()

    val onTrackerInfoChanged = PublishSubject.create<String>()

    private lateinit var initialOrder: Array<String>

    val isDirty: Boolean
        get() {
            if (currentOrderedTrackerViewModels.filterIndexed { index, model -> model.dao.position != index }.isNotEmpty()) {
                return !Arrays.equals(initialOrder, currentOrderedTrackerViewModels.map { it.dao.objectId!! }.toTypedArray())

            } else return false
        }

    override fun onInject(app: OTApp) {
        app.synchronizationComponent.inject(this)
    }

    override fun onUserAttached(newUserId: String) {
        super.onUserAttached(newUserId)
        trackerQueryResults = dbManager.get().makeTrackersOfUserQuery(newUserId, realm)
                .findAllSortedAsync(arrayOf("position", RealmDatabaseManager.FIELD_USER_CREATED_AT), arrayOf(Sort.ASCENDING, Sort.DESCENDING))

        trackerQueryResults?.addChangeListener(this)
    }

    override fun onChange(snapshot: RealmResults<OTTrackerDAO>, changeSet: OrderedCollectionChangeSet?) {
        if(changeSet == null)
        {
            //initial
            initialOrder = snapshot.map { it.objectId!! }.toTypedArray()

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

    fun moveTracker(from: Int, to: Int){
        currentOrderedTrackerViewModels.move(from, to)
        orderedTrackerViewModels.onNext(currentOrderedTrackerViewModels)
    }

    fun applyOrders(){
        if (isDirty) {
            realm.executeTransaction {
                currentOrderedTrackerViewModels.forEachIndexed { index, viewModel ->
                    if (viewModel.dao.position != index) {
                        viewModel.dao.position = index
                        viewModel.dao.synchronizedAt = null
                    }
                }
            }
            syncManager.registerSyncQueue(ESyncDataType.TRACKER, SyncDirection.UPLOAD)
        }

    }

    class OrderedTrackerViewModel(internal val dao: OTTrackerDAO) : IReadonlyObjectId {
        override val objectId: String?
            get() = dao.objectId
        val name: String get() = dao.name
        val color: Int get() = dao.color
    }
}