package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.app.Application
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.RealmResults
import io.realm.Sort
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
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

    private lateinit var initialOrder: MutableList<String>

    val isDirty: Boolean
        get() {
            if (currentOrderedTrackerViewModels.filterIndexed { index, model -> model.dao.position != index }.isNotEmpty()) {
                return !Arrays.equals(initialOrder.toTypedArray(), currentOrderedTrackerViewModels.map { it.dao.objectId!! }.toTypedArray())

            } else return false
        }

    override fun onInject(configuredContext: ConfiguredContext) {
        configuredContext.configuredAppComponent.inject(this)
    }

    override fun onUserAttached(newUserId: String) {
        super.onUserAttached(newUserId)
        trackerQueryResults = dbManager.get().makeTrackersOfUserVisibleQuery(newUserId, realm)
                .sort(arrayOf("position", BackendDbManager.FIELD_USER_CREATED_AT), arrayOf(Sort.ASCENDING, Sort.DESCENDING))
                .findAllAsync()

        trackerQueryResults?.addChangeListener(this)
    }

    override fun onChange(snapshot: RealmResults<OTTrackerDAO>, changeSet: OrderedCollectionChangeSet) {
        if (changeSet.state == OrderedCollectionChangeSet.State.INITIAL)
        {
            //initial
            initialOrder = snapshot.map { it.objectId!! }.toMutableList()

            currentOrderedTrackerViewModels.clear()
            currentOrderedTrackerViewModels.addAll(snapshot.map{OrderedTrackerViewModel(it)})
        }
        else{
            initialOrder.removeAll(changeSet.deletions.map { currentOrderedTrackerViewModels[it].objectId })
            currentOrderedTrackerViewModels.removeAll(changeSet.deletions.map { currentOrderedTrackerViewModels[it] })
            changeSet.insertions.forEach {
                currentOrderedTrackerViewModels.add(it, OrderedTrackerViewModel(snapshot[it]!!))
            }

            changeSet.changes.forEach {
                onTrackerInfoChanged.onNext(currentOrderedTrackerViewModels[it].objectId!!)
            }
        }
        orderedTrackerViewModels.onNext(currentOrderedTrackerViewModels)
    }

    override fun onUserDisposed() {
        super.onUserDisposed()
        trackerQueryResults?.removeAllChangeListeners()
    }

    fun moveTracker(from: Int, to: Int){
        currentOrderedTrackerViewModels.move(from, to)
        orderedTrackerViewModels.onNext(currentOrderedTrackerViewModels)
    }

    fun applyOrders(): Boolean {
        if (isDirty) {
            realm.executeTransaction {
                currentOrderedTrackerViewModels.forEachIndexed { index, viewModel ->
                    if (viewModel.dao.position != index) {
                        viewModel.dao.position = index
                        viewModel.dao.synchronizedAt = null
                    }
                }
            }
            syncManager.registerSyncQueue(ESyncDataType.TRACKER, SyncDirection.UPLOAD, ignoreDirtyFlags = false)
            return true
        } else return false
    }

    class OrderedTrackerViewModel(internal val dao: OTTrackerDAO) : IReadonlyObjectId {
        override val objectId: String? = dao.objectId
        val name: String get() = dao.name
        val color: Int get() = dao.color
    }
}