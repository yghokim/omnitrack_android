package kr.ac.snu.hcil.omnitrack.ui.pages.items

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTPendingItemBuilderDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.utils.RealmViewModel
import rx.subjects.BehaviorSubject

/**
 * Created by Young-Ho on 10/9/2017.
 */
class ItemEditionViewModel : RealmViewModel() {

    enum class ItemMode {
        Edit, New
    }

    enum class BuilderCreationMode {
        NewBuilder, Restored
    }

    var trackerDao: OTTrackerDAO? = null
        private set

    private lateinit var itemBuilderDao: OTPendingItemBuilderDAO

    val modeObservable = BehaviorSubject.create<ItemMode>(ItemMode.New)
    val builderCreationModeObservable = BehaviorSubject.create<BuilderCreationMode>()

    val builderWrapperObservable = BehaviorSubject.create<OTItemBuilderWrapperBase>()

    val unManagedAttributeListObservable = BehaviorSubject.create<List<OTAttributeDAO>>()

    var builderWrapper: OTItemBuilderWrapperBase
        get() {
            return builderWrapperObservable.value
        }
        private set(value) {
            if (builderWrapperObservable.value != value) {
                builderWrapperObservable.onNext(value)
            }
        }

    private val currentUnmanagedAttributeList = ArrayList<OTAttributeDAO>()

    var mode: ItemMode
        get() {
            return modeObservable.value
        }
        private set(value) {
            if (modeObservable.value != value) {
                modeObservable.onNext(value)
            }
        }

    fun init(trackerId: String, itemId: String?) {
        trackerDao = OTApplication.app.databaseManager.getTrackerQueryWithId(trackerId, realm).findFirst()
        subscriptions.clear()

        if (itemId != null) {
            mode = ItemMode.Edit
        } else {
            subscriptions.add(
                    OTApplication.app.databaseManager.getItemBuilderOfTracker(trackerId, OTPendingItemBuilderDAO.HOLDER_TYPE_INPUT_FORM, realm).subscribe { result ->
                        if (result.datum != null) {
                            //there is a pending itemBuilder.
                            this.itemBuilderDao = result.datum
                            builderCreationModeObservable.onNext(BuilderCreationMode.Restored)

                        } else {
                            //no pending itemBuilder.
                            realm.executeTransaction {
                                val newBuilderDao = realm.createObject(OTPendingItemBuilderDAO::class.java, (realm.where(OTPendingItemBuilderDAO::class.java).max("id")?.toLong() ?: 0) + 1)
                                newBuilderDao.tracker = trackerDao
                                newBuilderDao.holderType = OTPendingItemBuilderDAO.HOLDER_TYPE_INPUT_FORM
                                this.itemBuilderDao = newBuilderDao
                            }
                            builderCreationModeObservable.onNext(BuilderCreationMode.NewBuilder)
                        }

                        val unManagedTrackerDao = realm.copyFromRealm(trackerDao)!!
                        currentUnmanagedAttributeList.clear()
                        currentUnmanagedAttributeList.addAll(unManagedTrackerDao.attributes)
                        unManagedAttributeListObservable.onNext(currentUnmanagedAttributeList)

                        this.builderWrapper = OTItemBuilderWrapperBase(realm.copyFromRealm(this.itemBuilderDao), realm)

                        this.builderWrapper.autoComplete().subscribe { result ->
                            println(result)
                        }
                        mode = ItemMode.New
                    }
            )
        }
    }
}