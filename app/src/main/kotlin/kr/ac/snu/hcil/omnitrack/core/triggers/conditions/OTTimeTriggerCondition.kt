package kr.ac.snu.hcil.omnitrack.core.triggers.conditions

import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO

/**
 * Created by younghokim on 2017. 10. 18..
 */
class OTTimeTriggerCondition : ATriggerCondition(OTTriggerDAO.CONDITION_TYPE_TIME) {

    companion object {
        const val TIME_CONDITION_ALARM = 0
        const val TIME_CONDITION_INTERVAL = 1
    }

    var timeConditionType: Int = TIME_CONDITION_ALARM

    var alarmTimeHour: Byte = 17 // 0~23
    var alarmTimeMinute: Byte = 0 //0~59


    var intervalSeconds: Short = 60
    var intervalIsHourRangeUsed: Boolean = false
    var intervalHourRangeStart: Byte = 9
    var intervalHourRangeEnd: Byte = 24

    var dayOfWeekFlags: Byte = 0b1111111
    var endAt: Long? = null
}