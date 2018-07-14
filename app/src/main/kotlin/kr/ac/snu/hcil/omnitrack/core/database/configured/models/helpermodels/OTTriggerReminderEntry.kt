package kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels

import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTReminderAction

/**
 * Created by younghokim on 2017-11-13.
 */
open class OTTriggerReminderEntry : RealmObject() {

    companion object {
        const val FIELD_AUTO_EXPIRY_ALARM_ID = "autoExpiryAlarmId"
        const val FIELD_IS_AUTO_EXPIRY_ALARM_RESERVED_WHEN_DEVICE_ACTIVE = "isAutoExpiryAlarmReservedWhenDeviceActive"
    }

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

    //added in schema 2. used only in API < 27
    var autoExpiryAlarmId: Int? = null

    //added in schema 2. used only in API < 27
    @Index
    var isAutoExpiryAlarmReservedWhenDeviceActive: Boolean = false

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

    var serializedMetadata: String? = null
}