package kr.ac.snu.hcil.omnitrack.core.database.local.models.helpermodels

import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTriggerDAO

/**
 * Created by younghokim on 2017. 11. 11..
 */
open class OTTriggerSchedule : RealmObject() {
    companion object {
        const val FIELD_TRIGGER_ID = "triggerId"
        const val FIELD_FIRED = "fired"
        const val FIELD_SKIPPED = "skipped"
        const val FIELD_INTRINSIC_ALARM_TIME = "intrinsicAlarmTime"
    }

    @PrimaryKey
    var id: Long = 0

    @Index
    var intrinsicAlarmTime: Long = 0L

    /**
     * This flag is set true when the trigger was stopped by user or the system, breaking the chain.
     */
    var final: Boolean = false

    /**
     * this schedule is not repeated.
     * If true, the system is responsible to reschedule next alarm with the triggered time as a next pivot.
     */
    var oneShot: Boolean = false

    var trigger: OTTriggerDAO? = null


    @Index
    var fired: Boolean = false

    @Index
    var skipped: Boolean = false

    var parentAlarm: OTTriggerAlarmInstance? = null
}