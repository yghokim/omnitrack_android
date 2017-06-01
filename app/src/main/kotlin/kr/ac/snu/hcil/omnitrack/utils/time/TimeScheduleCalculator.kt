package kr.ac.snu.hcil.omnitrack.utils.time

import kr.ac.snu.hcil.omnitrack.utils.BitwiseOperationHelper

/**
 * Created by Young-Ho on 5/31/2017.
 */
abstract class TimeScheduleCalculator<T> where T : TimeScheduleCalculator<T> {

    private var availableDaysOfWeek = BooleanArray(7, { i -> true })
    var endAt: Long = Long.MAX_VALUE

    protected fun isAvailableDayOfWeek(dayOfWeek: Int): Boolean {
        return availableDaysOfWeek[dayOfWeek - 1]
    }

    fun setEndAt(endAt: Long): T {
        this.endAt = endAt
        return this as T
    }

    fun setAvailableDaysOfWeekFlag(flags: Int = 0b1111111): T {
        for (i in (0..6)) {
            availableDaysOfWeek[i] = BitwiseOperationHelper.getBooleanAt(flags, 6 - i)

        }

        return this as T
    }

    fun calculateNext(last: Long?, now: Long): Long? {
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