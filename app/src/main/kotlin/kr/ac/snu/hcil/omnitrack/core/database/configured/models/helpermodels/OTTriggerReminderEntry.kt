package kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels

import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTReminderAction

/**
 * Created by younghokim on 2017-11-13.
 */
open class OTTriggerReminderEntry : RealmObject() {
    @PrimaryKey
    var id: Long = 0

    var levelOrdinal: Int = 0

    @Index
    var systemIntrinsicId: Int = -1

    var triggerId: String? = null

    @Index
    var trackerId: String? = null

    @Index
    var dismissed: Boolean = false


    var intrinsicTriggerTime: Long = System.currentTimeMillis()

    @Index
    var notifiedAt: Long = System.currentTimeMillis()

    @Index
    var accessedAt: Long? = null

    @Index
    var loggedAt: Long? = null

    var level: OTReminderAction.NotificationLevel
        get() = OTReminderAction.NotificationLevel.values()[levelOrdinal]
        set(value)
        {
            levelOrdinal = value.ordinal
        }
}