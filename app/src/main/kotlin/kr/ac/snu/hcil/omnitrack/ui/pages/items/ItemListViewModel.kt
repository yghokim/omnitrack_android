package kr.ac.snu.hcil.omnitrack.ui.pages.items

import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.RealmResults
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.ItemComparator
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.utils.RealmViewModel
import rx.subjects.BehaviorSubject

/**
 * Created by Young-Ho on 10/12/2017.
 */
class ItemListViewModel : RealmViewModel(), OrderedRealmCollectionChangeListener<RealmResults<OTItemDAO>> {

    private var currentItemQueryResults: RealmResults<OTItemDAO>? = null
    private lateinit var trackerDao: OTTrackerDAO
    private lateinit var managedTrackerDao: OTTrackerDAO
    private lateinit var managedAttributeList: RealmResults<OTAttributeDAO>

    private val trackerNameObservable = BehaviorSubject.create<String>("")
    var trackerName: String
        get() = trackerNameObservable.value
        private set(value) {
            if (trackerNameObservable.value != value) {
                trackerNameObservable.onNext(value)
            }
        }

    private val sorterSetObservable = BehaviorSubject.create<List<ItemComparator>>()
    private val currentSorterSet = ArrayList<ItemComparator>()

    fun init(trackerId: String) {
        val dao = OTApplication.app.databaseManager.getTrackerQueryWithId(trackerId, realm).findFirst()

        if (dao != null) {
            managedTrackerDao = OTApplication.app.databaseManager.getTrackerQueryWithId(trackerId, realm).findFirstAsync()
            managedTrackerDao.addChangeListener<OTTrackerDAO> { snapshot ->
                println("tracker db changed in background")
            }
            managedAttributeList = OTApplication.app.databaseManager.getAttributeListQuery(trackerId, realm).findAllAsync()
            managedAttributeList.addChangeListener { snapshot, changeSet ->
                println("tracker attribute list db changed in background")
            }

            refreshTracker(realm.copyFromRealm(dao))
        }
    }

    private fun refreshTracker(trackerDao: OTTrackerDAO) {
        this.trackerDao = trackerDao
        trackerName = trackerDao.name

        currentSorterSet.clear()
        this.trackerDao.attributes.forEach {
            currentSorterSet += it.getHelper().getSupportedSorters(it)
        }
        sorterSetObservable.onNext(currentSorterSet)
        currentItemQueryResults?.removeAllChangeListeners()
        currentItemQueryResults = OTApplication.app.databaseManager.makeItemsQuery(trackerDao.objectId, null, null, realm).findAllAsync()
        currentItemQueryResults?.addChangeListener(this)
    }

    override fun onChange(snapshot: RealmResults<OTItemDAO>, changeSet: OrderedCollectionChangeSet?) {
        if (changeSet == null) {
            //initial
        }


    }
}