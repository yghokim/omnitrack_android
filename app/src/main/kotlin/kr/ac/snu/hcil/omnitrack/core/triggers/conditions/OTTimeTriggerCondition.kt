package kr.ac.snu.hcil.omnitrack.core.triggers.conditions

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO

/**
 * Created by younghokim on 2017. 10. 18..
 */
class OTTimeTriggerCondition : ATriggerCondition(OTTriggerDAO.CONDITION_TYPE_TIME) {

    class TimeTriggerConditionTypeAdapter : TypeAdapter<OTTimeTriggerCondition>() {
        override fun read(reader: JsonReader): OTTimeTriggerCondition {
            val condition = OTTimeTriggerCondition()

            reader.beginObject()

            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "cType" -> condition.timeConditionType = reader.nextInt().toByte()
                    "aHr" -> condition.alarmTimeHour = reader.nextInt().toByte()
                    "aMin" -> condition.alarmTimeMinute = reader.nextInt().toByte()
                    "iSec" -> condition.intervalSeconds = reader.nextInt().toShort()
                    "iRanged" -> condition.intervalIsHourRangeUsed = reader.nextBoolean()
                    "iStartHr" -> condition.intervalHourRangeStart = reader.nextInt().toByte()
                    "iEndHr" -> condition.intervalHourRangeEnd = reader.nextInt().toByte()
                    "repeat" -> condition.isRepeated = reader.nextBoolean()
                    "dow" -> condition.dayOfWeekFlags = reader.nextInt().toByte()
                    "endAt" -> if (reader.peek() == JsonToken.STRING) {
                        condition.endAt = reader.nextLong()
                    } else reader.skipValue()
                    else -> reader.skipValue()
                }
            }

            reader.endObject()

            return condition
        }

        override fun write(out: JsonWriter, value: OTTimeTriggerCondition) {
            out.beginObject()

            out.name("cType").value(value.timeConditionType)
            out.name("aHr").value(value.alarmTimeHour)
            out.name("aMin").value(value.alarmTimeMinute)
            out.name("iSec").value(value.intervalSeconds)
            out.name("iRanged").value(value.intervalIsHourRangeUsed)
            out.name("iStartHr").value(value.intervalHourRangeStart)
            out.name("iEndHr").value(value.intervalHourRangeEnd)
            out.name("repeat").value(value.isRepeated)
            out.name("dow").value(value.dayOfWeekFlags)
            out.name("endAt").value(value.endAt)

            out.endObject()
        }

    }

    companion object {
        const val TIME_CONDITION_ALARM: Byte = 0
        const val TIME_CONDITION_INTERVAL: Byte = 1

        val typeAdapter: TimeTriggerConditionTypeAdapter by lazy { TimeTriggerConditionTypeAdapter() }
    }

    var timeConditionType: Byte = TIME_CONDITION_ALARM

    var alarmTimeHour: Byte = 17 // 0~23
    var alarmTimeMinute: Byte = 0 //0~59

    var intervalSeconds: Short = 60
    var intervalIsHourRangeUsed: Boolean = false
    var intervalHourRangeStart: Byte = 9
    var intervalHourRangeEnd: Byte = 24

    var isRepeated: Boolean = false

    var dayOfWeekFlags: Byte = 0b1111111
    var endAt: Long? = null

    override fun getSerializedString(): String {
        return typeAdapter.toJson(this)
    }


    override fun isConfigurationValid(validationErrorMessages: MutableList<CharSequence>?): Boolean {
        if (timeConditionType == TIME_CONDITION_INTERVAL && intervalSeconds <= 0.toShort()) {
            validationErrorMessages?.add(OTApp.getString(R.string.msg_trigger_error_interval_not_0))
            return false
        } else return true
    }

    override fun equals(other: Any?): Boolean {
        return if (other === this) {
            true
        } else if (other is OTTimeTriggerCondition) {
            timeConditionType == other.timeConditionType
                    && alarmTimeHour == other.alarmTimeHour
                    && alarmTimeMinute == other.alarmTimeMinute
                    && intervalSeconds == other.intervalSeconds
                    && intervalIsHourRangeUsed == other.intervalIsHourRangeUsed
                    && intervalHourRangeStart == other.intervalHourRangeStart
                    && intervalHourRangeEnd == other.intervalHourRangeEnd
                    && isRepeated == other.isRepeated
                    && dayOfWeekFlags == other.dayOfWeekFlags
                    && endAt == other.endAt
        } else false
    }
}