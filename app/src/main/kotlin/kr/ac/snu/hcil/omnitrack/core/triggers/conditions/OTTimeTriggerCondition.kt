package kr.ac.snu.hcil.omnitrack.core.triggers.conditions

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO

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
                    "cType" -> condition.timeConditionType = reader.nextInt()
                    "aHr" -> condition.alarmTimeHour = reader.nextInt().toByte()
                    "aMin" -> condition.alarmTimeMinute = reader.nextInt().toByte()
                    "iSec" -> condition.intervalSeconds = reader.nextInt().toShort()
                    "iRanged" -> condition.intervalIsHourRangeUsed = reader.nextBoolean()
                    "iStartHr" -> condition.intervalHourRangeStart = reader.nextInt().toByte()
                    "iEndHr" -> condition.intervalHourRangeEnd = reader.nextInt().toByte()
                    "dow" -> condition.dayOfWeekFlags = reader.nextInt().toByte()
                    "endAt" -> condition.endAt = reader.nextLong()
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
            out.name("dow").value(value.dayOfWeekFlags)
            out.name("endAt").value(value.endAt)

            out.endObject()
        }

    }

    companion object {
        const val TIME_CONDITION_ALARM = 0
        const val TIME_CONDITION_INTERVAL = 1

        val typeAdapter: TimeTriggerConditionTypeAdapter by lazy { TimeTriggerConditionTypeAdapter() }
        val parser: Gson by lazy {
            GsonBuilder().registerTypeAdapter(OTTimeTriggerCondition::class.java, typeAdapter).create()
        }
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


    override fun getSerializedString(): String? {
        return parser.toJson(this, OTTimeTriggerCondition::class.java)
    }
}