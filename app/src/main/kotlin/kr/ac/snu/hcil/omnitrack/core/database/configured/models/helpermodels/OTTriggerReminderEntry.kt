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

    /**
     * Notification level enum
     */
    var levelOrdinal: Int = 0

    /**
     * an Id of system-wise entity for this reminder.
     * for Simple notification, this is an alarmId.
     */
    @Index
    var systemIntrinsicId: Int = -1

    var triggerId: String? = null

    @Index
    var trackerId: String? = null

    @Index
    var dismissed: Boolean = false

    var autoExpireAt: Long = Long.MAX_VALUE
    var timeoutDuration: Int? = null

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