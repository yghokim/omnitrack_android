package kr.ac.snu.hcil.omnitrack.core.triggers.database

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index

/**
 * Created by younghokim on 2017-06-01.
 */
open class TriggerSchedule : RealmObject() {

    companion object {
        const val FIELD_TRIGGER_ID = "triggerId"
        const val FIELD_FIRED = "fired"
        const val FIELD_SKIPPED = "skipped"
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

    var parentAlarm: AlarmInstance? = null
}


open class AlarmInstance : RealmObject() {
    companion object {
        const val FIELD_ALARM_ID = "alarmId"
        const val FIELD_FIRED = "fired"
        const val FIELD_SKIPPED = "skipped"
    }


    @Index
    var reservedAlarmTime: Long = 0L

    @Index
    var alarmId: Int = -1

    var fired: Boolean = false
    var skipped: Boolean = false

    var triggerSchedules = RealmList<TriggerSchedule>()
}