package kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey

/**
 * Created by younghokim on 2017. 11. 11..
 */
open class OTTriggerAlarmInstance : RealmObject() {

    data class AlarmInfo(val systemAlarmId: Int, val reservedAlarmTime: Long)

    companion object {
        const val FIELD_ALARM_ID = "alarmId"
        const val FIELD_FIRED = "fired"
        const val FIELD_SKIPPED = "skipped"
        const val FIELD_RESERVED_WHEN_DEVICE_ACTIVE = "isReservedWhenDeviceActive"
        const val FIELD_TRIGGER_SCHEDULES = "triggerSchedules"
    }

    @PrimaryKey
    var id: Long = 0

    @Index
    var reservedAlarmTime: Long = 0L

    @Index
    var alarmId: Int = -1

    var userId: String? = null

    var fired: Boolean = false
    var skipped: Boolean = false


    //added in schemaVersion 1
    var isReservedWhenDeviceActive: Boolean = false

    @LinkingObjects("parentAlarm")
    val triggerSchedules: RealmResults<OTTriggerSchedule>? = null

    fun getInfo(): AlarmInfo {
        return AlarmInfo(alarmId, reservedAlarmTime)
    }
}