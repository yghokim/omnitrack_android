package kr.ac.snu.hcil.omnitrack.core.datatypes

import kr.ac.snu.hcil.omnitrack.utils.serialization.IStringSerializable
import java.util.*

/**
 * Created by younghokim on 16. 7. 21..
 */
class TimePoint : IStringSerializable {

    var timestamp: Long = 0
    var timezone: TimeZone = TimeZone.getDefault()

    constructor(timestamp: Long, timezoneName: String) {

    }

    constructor(serialized: String) {
        fromSerializedString(serialized)
    }

    override fun getSerializedString(): String {
        return "${timestamp}@${timezone.id}"
    }

    override fun fromSerializedString(serialized: String): Boolean {
        try {
            val parts = serialized.split("@")
            timestamp = parts[0].toLong()
            timezone = TimeZone.getTimeZone(parts[1])
            return true
        } catch(e: Exception) {
            return false
        }
    }

}