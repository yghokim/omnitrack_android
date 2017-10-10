package kr.ac.snu.hcil.omnitrack.ui.pages.items

import kr.ac.snu.hcil.omnitrack.OTApplication
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

    private var trackerDao: OTTrackerDAO? = null
    private var itemBuilderDao: OTPendingItemBuilderDAO? = null

    val modeObservable = BehaviorSubject.create<ItemMode>(ItemMode.New)
    val builderCreationModeObservable = BehaviorSubject.create<BuilderCreationMode>()

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
        trackerDao = OTApplication.app.databaseManager.getTrackerQueryWithId(trackerId, realm).findFirstAsync()
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

                        mode = ItemMode.New
                    }
            )
        }
    }
}