package kr.ac.snu.hcil.omnitrack.core.triggers

import io.realm.RealmList
import kr.ac.snu.hcil.omnitrack.core.database.local.OTStringStringEntryDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTTriggerAction

/**
 * Created by younghokim on 2017. 10. 17..
 */
abstract class OTTriggerWrapper(val dao: OTTriggerDAO) {
    val objectId: String? get() = dao.objectId
    var alias: String
        get() = dao.alias
        set(value) {
            if (dao.alias != value) {
                dao.alias = value
            }
        }

    abstract val conditionType: Int

    abstract val action: OTTriggerAction

    //Device-only properties===========
    //When synchronizing them, convey them with corresponding device local ids.
    var lastTriggeredTime: Long?
        get() = dao.lastTriggeredTime
        set(value) {
            if (dao.lastTriggeredTime != value) {
                dao.lastTriggeredTime = value
            }
        }

    var isOn: Boolean
        get() = dao.isOn
        set(value) {
            if (dao.isOn != value) {
                dao.isOn = value
            }
        }

    //=================================

    var properties = RealmList<OTStringStringEntryDAO>()
    var trackers = RealmList<OTTrackerDAO>()
}