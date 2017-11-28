package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.app.Application
import io.realm.RealmQuery
import kr.ac.snu.hcil.omnitrack.core.database.local.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.AManagedTriggerListViewModel

/**
 * Created by younghokim on 2017. 10. 21..
 */
class LoggingTriggerListViewModel(app: Application) : AManagedTriggerListViewModel(app) {

    var userId: String? = null
        private set

    fun init(userId: String) {
        if (this.userId != userId) {
            this.userId = userId
            init()
        }
    }

    override fun hookTriggerQuery(originalQuery: RealmQuery<OTTriggerDAO>): RealmQuery<OTTriggerDAO> {
        return originalQuery.equalTo(BackendDbManager.FIELD_USER_ID, userId).equalTo("actionType", OTTriggerDAO.ACTION_TYPE_LOG)

    }
}