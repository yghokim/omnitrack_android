package kr.ac.snu.hcil.omnitrack.utils.time

import android.text.format.DateUtils
import kr.ac.snu.hcil.omnitrack.utils.getDayOfWeek
import kr.ac.snu.hcil.omnitrack.utils.getHourOfDay
import kr.ac.snu.hcil.omnitrack.utils.setHourOfDay
import java.util.*

/**
 * Created by Young-Ho on 5/31/2017.
 */
class IntervalTimeScheduleCalculator : TimeScheduleCalculator<IntervalTimeScheduleCalculator>() {


    var intervalMillis: Long = 0

    var fromHourOfDay: Int = 0
    var toHourOfDay: Int = 24

    val cacheCalendar = GregorianCalendar.getInstance()

    fun setHourBoundingRange(fromHourOfDay: Int, toHourOfDay: Int): IntervalTimeScheduleCalculator {
        if ((fromHourOfDay < 0 || fromHourOfDay > 24) || (toHourOfDay < 0 || toHourOfDay > 24)) {
            throw IllegalArgumentException("boundingHours are out of range.")
        }

        this.fromHourOfDay = fromHourOfDay
        this.toHourOfDay = toHourOfDay

        return this
    }

    fun setInterval(millis: Long): IntervalTimeScheduleCalculator {
        this.intervalMillis = millis
        return this
    }

    private fun getNearestFutureNextIntervalTime(pivot: Long, now: Long, interval: Long): Long {
        val skipIntervalCount = (now - pivot) / interval

        return pivot + (skipIntervalCount + 1) * interval
    }

    override fun calculateInfiniteNextTime(last: Long?, now: Long): Long {

        val realPivot: Long = last ?: now
        val isHourRangeBound = fromHourOfDay != toHourOfDay

        val pivotCalendar = GregorianCalendar.getInstance()
        pivotCalendar.timeInMillis = realPivot

        if (isHourRangeBound) {
            val boundRangeOffset: Int = fromHourOfDay
            val boundRangeLength: Int = if (toHourOfDay > fromHourOfDay) {
                //normal case
                toHourOfDay - fromHourOfDay
            } else {
                24 - fromHourOfDay + toHourOfDay
            }

            val realPivotContainingRangeStart = checkTimePointWithinAvailableRange(realPivot, boundRangeOffset, boundRangeLength)
            if (realPivotContainingRangeStart == null) {
                //return start point of the next available range
                return getStartOfNextAvailableRange(now, boundRangeOffset, boundRangeLength)
            } else {
                val next = getNearestFutureNextIntervalTime(realPivot, now, intervalMillis)
                if (checkTimePointWithinAvailableRange(next, boundRangeOffset, boundRangeLength) != null) {
                    return next
                } else {
                    //return start point of the next available range
                    return getStartOfNextAvailableRange(now, boundRangeOffset, boundRangeLength)
                }
            }


        } else {
            val next = getNearestFutureNextIntervalTime(realPivot, now, intervalMillis)
            cacheCalendar.timeInMillis = next
            val dayOfWeekOfNextTime = cacheCalendar.getDayOfWeek()
            var dayPlus = 0
            while (!isAvailableDayOfWeek((dayOfWeekOfNextTime + dayPlus) % 7)) {
                dayPlus++
            }

            if (dayPlus == 0) {
                return next
            } else {
                //return 0:00 of next available dayOfWeek
                return TimeHelper.addDays(TimeHelper.cutTimePartFromEpoch(next), dayPlus)
            }
        }
    }

    private fun getStartOfNextAvailableRange(pivot: Long, rangeOffsetHour: Int, rangeLengthHour: Int): Long {
        cacheCalendar.timeInMillis = TimeHelper.cutTimePartFromEpoch(pivot)
        var currentStartPoint: Long
        while (true) {
            currentStartPoint = cacheCalendar.timeInMillis + rangeOffsetHour * DateUtils.HOUR_IN_MILLIS
            val dow = cacheCalendar.getDayOfWeek()
            if (isAvailableDayOfWeek(dow) && currentStartPoint > pivot) {
                break
            } else {
                cacheCalendar.add(Calendar.DAY_OF_YEAR, 1)
                continue
            }
        }
        return currentStartPoint
    }

    private fun checkTimePointWithinAvailableRange(timePoint: Long, rangeOffsetHour: Int, rangeLengthHour: Int): Long? {
        val pivotCalendar = GregorianCalendar.getInstance()
        pivotCalendar.timeInMillis = timePoint
        if (fromHourOfDay < toHourOfDay) {
            //range is within a single day: simple range calculation
            val dow = pivotCalendar.getDayOfWeek()
            if (isAvailableDayOfWeek(dow)) {
                val hourOfDay = pivotCalendar.getHourOfDay()
                if (hourOfDay in fromHourOfDay..(toHourOfDay - 1)) {
                    val startOfPivotDate = TimeHelper.cutTimePartFromEpoch(timePoint)
                    cacheCalendar.timeInMillis = startOfPivotDate
                    cacheCalendar.setHourOfDay(fromHourOfDay, true)
                    return cacheCalendar.timeInMillis
                } else {
                    return null
                }
            } else {
                return null
            }
        } else {
            //boundHourRange is reversed. check the day and the previous day.
            var currentPivotStartValue: Long? = null
            for (offset in -1..0) {
                cacheCalendar.timeInMillis = TimeHelper.cutTimePartFromEpoch(timePoint) + offset * DateUtils.DAY_IN_MILLIS
                if (isAvailableDayOfWeek(cacheCalendar.getDayOfWeek())) {
                    val rangeStartAt = cacheCalendar.timeInMillis + rangeOffsetHour * DateUtils.HOUR_IN_MILLIS
                    if (timePoint in (rangeStartAt..rangeStartAt + rangeLengthHour * DateUtils.HOUR_IN_MILLIS - 1)) {
                        currentPivotStartValue = rangeStartAt
                        break
                    }
                }
            }
            return currentPivotStartValue
        }
    }
}