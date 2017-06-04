package kr.ac.snu.hcil.omnitrack.core.triggers.database

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey

/**
 * Created by younghokim on 2017-06-01.
 */

data class TriggerSchedulePOJO(val triggerId: String, val intrinsicAlarmTime: Long, val oneShot: Boolean)

open class TriggerSchedule : RealmObject() {

    companion object {
        const val FIELD_TRIGGER_ID = "triggerId"
        const val FIELD_FIRED = "fired"
        const val FIELD_SKIPPED = "skipped"
        const val FIELD_INTRINSIC_ALARM_TIME = "intrinsicAlarmTime"
    }

    @Index
    var intrinsicAlarmTime: Long = 0L

    @Index
    var triggerId: String = ""

    var oneShot: Boolean = false

    @Index
    var fired: Boolean = false

    @Index
    var skipped: Boolean = false

    var parentAlarmId: Long? = null

    fun getPOJO(): TriggerSchedulePOJO {
        return TriggerSchedulePOJO(triggerId, intrinsicAlarmTime, oneShot)
    }
}

data class AlarmInfo(val systemAlarmId: Int, val reservedAlarmTime: Long) {}

open class AlarmInstance : RealmObject() {
    companion object {
        const val FIELD_ALARM_ID = "alarmId"
        const val FIELD_FIRED = "fired"
        const val FIELD_SKIPPED = "skipped"
    }

    @PrimaryKey
    var id: Long = 0

    @Index
    var reservedAlarmTime: Long = 0L

    @Index
    var alarmId: Int = -1

    var fired: Boolean = false
    var skipped: Boolean = false

    var triggerSchedules = RealmList<TriggerSchedule>()

    fun getInfo(): AlarmInfo {
        return AlarmInfo(alarmId, reservedAlarmTime)
    }
}