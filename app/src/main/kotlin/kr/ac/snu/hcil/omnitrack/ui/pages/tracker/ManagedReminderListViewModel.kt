package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import io.realm.RealmQuery
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.AManagedTriggerListViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.TriggerInterfaceOptions

/**
 * Created by younghokim on 2017. 10. 22..
 */
class ManagedReminderListViewModel : AManagedTriggerListViewModel() {

    val trackerId: String?
        get() {
            return trackerDao?.objectId
        }

    var trackerDao: OTTrackerDAO? = null
        private set

    private lateinit var currentDefaultTriggerInterfaceOptions: TriggerInterfaceOptions


    fun init(trackerId: String) {
        if (this.trackerId != trackerId) {
            trackerDao = realm.where(OTTrackerDAO::class.java).equalTo("objectId", trackerId).findFirst()
            currentDefaultTriggerInterfaceOptions = TriggerInterfaceOptions(false, arrayOf(trackerId), arrayOf(OTTriggerDAO.CONDITION_TYPE_TIME), OTTriggerDAO.ACTION_TYPE_REMIND)
            init()
        }
    }

    override fun hookTriggerQuery(originalQuery: RealmQuery<OTTriggerDAO>): RealmQuery<OTTriggerDAO> {
        return originalQuery.equalTo("userId", OTAuthManager.userId).equalTo("trackers.objectId", trackerId!!).equalTo("actionType", OTTriggerDAO.ACTION_TYPE_REMIND)
    }

    override fun beforeAddNewTrigger(daoToAdd: OTTriggerDAO) {
        super.beforeAddNewTrigger(daoToAdd)

        if (daoToAdd.trackers.find { it.objectId == trackerDao?.objectId } == null) {
            daoToAdd.trackers.add(realm.copyFromRealm(trackerDao))
        }
    }

    override val emptyMessageResId: Int = R.string.msg_reminder_empty

    override val defaultTriggerInterfaceOptions: TriggerInterfaceOptions
        get() = currentDefaultTriggerInterfaceOptions

}