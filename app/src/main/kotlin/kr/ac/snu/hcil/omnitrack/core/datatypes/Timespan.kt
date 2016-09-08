package kr.ac.snu.hcil.omnitrack.core.datatypes


import kr.ac.snu.hcil.omnitrack.utils.serialization.IStringSerializable
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */
class TimeSpan : IStringSerializable {

    var from: Long = 0
    var duration: Int = 0
    var timeZone: TimeZone = TimeZone.getDefault()

    val to: Long get() = from + duration.toLong()

    constructor(from: Long = System.currentTimeMillis(), duration: Int = 0, timeZone: TimeZone = TimeZone.getDefault()) {
        this.from = from
        this.duration = duration
        this.timeZone = timeZone
    }

    constructor(from: Long, to: Long, timeZone: TimeZone = TimeZone.getDefault()) : this(from, (to - from).toInt())

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
            duration = parts[2].toInt()

            return true
        } catch(e: Exception) {
            return false
        }
    }

}