package kr.ac.snu.hcil.omnitrack.utils.time

import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.utils.BitwiseOperationHelper
import kr.ac.snu.hcil.omnitrack.utils.WritablePair

/**
 * Created by Young-Ho on 5/31/2017.
 */
abstract class TimeScheduleCalculator {

    private var availableDaysOfWeek = BooleanArray(7, { i -> true })
    var endAt: Long = Long.MAX_VALUE

    protected fun isAvailableDayOfWeek(dayOfWeek: Int): Boolean {
        return availableDaysOfWeek[dayOfWeek - 1]
    }

    fun setEndAt(endAt: Long): TimeScheduleCalculator {
        this.endAt = endAt
        return this
    }

    fun setAvailableDaysOfWeekFlag(flags: Int = 0b1111111): TimeScheduleCalculator {
        for (i in (0..6)) {
            if (flags == 0) {
                availableDaysOfWeek[i] = true
            } else availableDaysOfWeek[i] = BitwiseOperationHelper.getBooleanAt(flags, 6 - i)
        }

        return this
    }

    open fun calculateNext(last: Long?, now: Long): WritablePair<Long, JsonObject?>? {
        if (now > endAt || availableDaysOfWeek.find { it == true } == null) {
            return null
        } else {
            val next = calculateInfiniteNextTime(last, now)
            if (next == null || next.first > endAt) {
                return null
            } else return next
        }

    }

    protected abstract fun calculateInfiniteNextTime(last: Long?, now: Long): WritablePair<Long, JsonObject?>?
}