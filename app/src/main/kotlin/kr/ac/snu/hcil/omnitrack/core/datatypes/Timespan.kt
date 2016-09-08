package kr.ac.snu.hcil.omnitrack.core.datatypes


import android.text.format.DateUtils
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.utils.serialization.IStringSerializable
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */
class TimeSpan : IStringSerializable {

    var from: Long = 0
    var duration: Long = 0
    var timeZone: TimeZone = TimeZone.getDefault()

    val to: Long get() = from + duration.toLong()

    companion object {
        fun fromDuration(from: Long = System.currentTimeMillis(), duration: Long = 0L, timeZone: TimeZone = TimeZone.getDefault()): TimeSpan {
            return TimeSpan(from, duration, timeZone)
        }

        fun fromPoints(from: Long = System.currentTimeMillis(), to: Long = from, timeZone: TimeZone = TimeZone.getDefault()): TimeSpan {
            return TimeSpan(from, to - from, timeZone)
        }
    }


    constructor() {
        from = System.currentTimeMillis()
    }

    private constructor(from: Long = System.currentTimeMillis(), duration: Long = 0L, timeZone: TimeZone = TimeZone.getDefault()) {
        this.from = from
        this.duration = duration
        this.timeZone = timeZone
    }


    override fun toString(): String {
        return "TimeSpan : ${DateUtils.formatDateTime(OTApplication.app, from, DateUtils.FORMAT_ABBREV_ALL)} ~ ${DateUtils.formatDateTime(OTApplication.app, to, DateUtils.FORMAT_ABBREV_ALL)}"
    }

    constructor(serialized: String) {
        fromSerializedString(serialized)
    }

    override fun getSerializedString(): String {
        return "${from}@${duration}@${timeZone.id}"
    }

    override fun fromSerializedString(serialized: String): Boolean {
        try {
            val parts = serialized.split("@")
            from = parts[0].toLong()
            timeZone = TimeZone.getTimeZone(parts[1])
            duration = parts[2].toLong()

            return true
        } catch(e: Exception) {
            return false
        }
    }

}