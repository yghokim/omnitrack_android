package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import io.realm.RealmQuery
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.ATriggerListViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.TriggerInterfaceOptions

/**
 * Created by younghokim on 2017. 10. 22..
 */
class ReminderListViewModel : ATriggerListViewModel() {

    private var trackerId: String? = null
    private lateinit var currentDefaultTriggerInterfaceOptions: TriggerInterfaceOptions

    fun init(trackerId: String) {
        if (this.trackerId != trackerId) {
            this.trackerId = trackerId
            currentDefaultTriggerInterfaceOptions = TriggerInterfaceOptions(false, arrayOf(trackerId), arrayOf(OTTriggerDAO.CONDITION_TYPE_TIME), OTTriggerDAO.ACTION_TYPE_REMIND)
            init()
        }
    }

    override fun hookTriggerQuery(originalQuery: RealmQuery<OTTriggerDAO>): RealmQuery<OTTriggerDAO> {
        return originalQuery.equalTo("trackers.objectId", trackerId!!).equalTo("actionType", OTTriggerDAO.ACTION_TYPE_REMIND)
    }

    override val emptyMessageResId: Int = R.string.msg_reminder_empty

    override val defaultTriggerInterfaceOptions: TriggerInterfaceOptions
        get() = currentDefaultTriggerInterfaceOptions
}