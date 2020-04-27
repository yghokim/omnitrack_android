package kr.ac.snu.hcil.omnitrack.core.fields.validators

import com.google.gson.JsonArray
import kr.ac.snu.hcil.android.common.time.TimeHelper
import kr.ac.snu.hcil.omnitrack.core.types.TimePoint
import kr.ac.snu.hcil.omnitrack.core.types.TimeSpan


object ValidationTypes {
    const val SameDayTimeInputValidator = "same_day"
}

object ValidatorManager {
    fun isValid(type: String, params: JsonArray? = null, value: Any?, pivotTime: Long? = null): Boolean {
        when (type) {
            ValidationTypes.SameDayTimeInputValidator -> {
                if (value is TimeSpan || value is TimePoint) {
                    if (value is TimeSpan) {
                        return TimeHelper.isSameDay(pivotTime!!, value.from, value.timeZone) && TimeHelper.isSameDay(pivotTime, value.to, value.timeZone)
                    } else if (value is TimePoint) {
                        return TimeHelper.isSameDay(pivotTime!!, value.timestamp, value.timeZone)
                    } else return false
                } else return false
            }
            else -> return false
        }
    }
}