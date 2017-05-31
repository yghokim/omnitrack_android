package kr.ac.snu.hcil.omnitrack.utils.time

/**
 * Created by Young-Ho on 5/31/2017.
 */
abstract class TimeScheduleCalculator {

    var availableDaysOfWeek = BooleanArray(7, { i -> true })
    var endAt: Long = Long.MAX_VALUE

    fun calculateNext(last: Long, now: Long): Long? {
        if (now > endAt) {
            return null
        } else {
            val next = calculateInfiniteNextTime(last, now)
            if (next > endAt) {
                return null
            } else return next
        }

    }

    protected abstract fun calculateInfiniteNextTime(last: Long?, now: Long): Long
}