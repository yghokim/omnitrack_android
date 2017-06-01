package kr.ac.snu.hcil.omnitrack.core.triggers

import io.realm.RealmObject
import io.realm.annotations.Index

/**
 * Created by younghokim on 2017-06-01.
 */
class TriggerAlarmScheduleUnit : RealmObject() {

    @Index
    var reservedAlarmTime: Long = 0L

    @Index
    var triggerObjectId: String = ""

    @Index
    var fired: Boolean = false

    @Index
    var skipped: Boolean = false
}