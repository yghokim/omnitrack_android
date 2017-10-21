package kr.ac.snu.hcil.omnitrack.ui.pages.home

import io.realm.RealmQuery
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.ATriggerListViewModel

/**
 * Created by younghokim on 2017. 10. 21..
 */
class LoggingTriggerListViewModel : ATriggerListViewModel() {

    var userId: String? = null
        private set

    fun init(userId: String) {
        if (this.userId != userId) {
            init()
        }
    }

    override fun hookTriggerQuery(originalQuery: RealmQuery<OTTriggerDAO>): RealmQuery<OTTriggerDAO> =
            originalQuery.equalTo(RealmDatabaseManager.FIELD_USER_ID, userId).equalTo("actionType", OTTriggerDAO.ACTION_TYPE_LOG)
}