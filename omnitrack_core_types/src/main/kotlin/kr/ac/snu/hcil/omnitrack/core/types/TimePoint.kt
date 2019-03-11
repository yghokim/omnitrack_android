package kr.ac.snu.hcil.omnitrack.core.types

import kr.ac.snu.hcil.android.common.serialization.IStringSerializable
import kr.ac.snu.hcil.android.common.time.toDatetimeString
import java.util.*

/**
 * Created by younghokim on 16. 7. 21..
 */
class TimePoint : IStringSerializable, Comparable<TimePoint> {
    override fun compareTo(other: TimePoint): Int {
        return timestamp.compareTo(other.timestamp)
    }

    var timestamp: Long = 0
    var timeZone: TimeZone = TimeZone.getDefault()

    constructor(timestamp: Long, timezoneId: String) {
        this.timestamp = timestamp
        this.timeZone = TimeZone.getTimeZone(timezoneId)
    }

    constructor() : this(System.currentTimeMillis(), TimeZone.getDefault().id)

    constructor(serialized: String) {
        fromSerializedString(serialized)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        else if (other is TimePoint) {
            return other.timestamp == this.timestamp && other.timeZone == this.timeZone
        } else return false
    }

    override fun getSerializedString(): String {
        return "$timestamp@${timeZone.id}"
    }

    override fun fromSerializedString(serialized: String): Boolean {
        try {
            val parts = serialized.split("@")
            timestamp = parts[0].toLong()
            timeZone = TimeZone.getTimeZone(parts[1])
            return true
        } catch (e: Exception) {
            return false
        }
    }

    override fun toString(): String {
        return "TimePoint{${timestamp.toDatetimeString()}, ${timeZone.displayName}}"
    }
}