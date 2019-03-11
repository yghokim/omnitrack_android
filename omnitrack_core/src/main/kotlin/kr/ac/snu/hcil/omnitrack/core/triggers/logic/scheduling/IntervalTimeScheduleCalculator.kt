package kr.ac.snu.hcil.omnitrack.core.triggers.logic.scheduling

import android.text.format.DateUtils
import com.google.gson.JsonObject
import kr.ac.snu.hcil.android.common.containers.WritablePair
import kr.ac.snu.hcil.android.common.time.TimeHelper
import kr.ac.snu.hcil.android.common.time.getDayOfWeek
import kr.ac.snu.hcil.android.common.time.getHourOfDay
import kr.ac.snu.hcil.android.common.time.setHourOfDay
//import kr.ac.snu.hcil.omnitrack.OTApp
//import kr.ac.snu.hcil.omnitrack.core.database.LoggingDbHelper
import java.util.*

/**
 * Created by Young-Ho on 5/31/2017.
 */
class IntervalTimeScheduleCalculator : TimeScheduleCalculator() {

    var intervalMillis: Long = 0

    var fromHourOfDay: Int = 0
    var toHourOfDay: Int = 0

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

    fun setNoLimit(): IntervalTimeScheduleCalculator {
        this.fromHourOfDay = 0
        this.toHourOfDay = 0
        this.endAt = Long.MAX_VALUE
        return this
    }

    private fun getNearestFutureNextIntervalTime(pivot: Long, now: Long, interval: Long): Long {
        val skipIntervalCount = (now - pivot) / interval

        return pivot + (skipIntervalCount + 1) * interval
    }

    override fun calculateInfiniteNextTime(last: Long?, now: Long): WritablePair<Long, JsonObject?>? {

        println("trigger time last: $last, now: $now")
        //OTApp.logger.writeSystemLog("calc next alarm. last: ${last?.let { LoggingDbHelper.TIMESTAMP_FORMAT.format(Date(last)) }}, now: ${LoggingDbHelper.TIMESTAMP_FORMAT.format(Date(now))}", "IntervalTimeScheduleCalculator")
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
                return WritablePair(getStartOfNextAvailableRange(now, boundRangeOffset, boundRangeLength), null)
            } else {
                val next = getNearestFutureNextIntervalTime(realPivot, now, intervalMillis)
                if (checkTimePointWithinAvailableRange(next, boundRangeOffset, boundRangeLength) != null) {
                    return WritablePair(next, null)
                } else {
                    //return start point of the next available range
                    return WritablePair(getStartOfNextAvailableRange(now, boundRangeOffset, boundRangeLength), null)
                }
            }


        } else {
            val next = getNearestFutureNextIntervalTime(realPivot, now, intervalMillis)
            cacheCalendar.timeInMillis = next
            val dayOfWeekOfNextTime = cacheCalendar.getDayOfWeek()
            var dayPlus = 0
            while (!isAvailableDayOfWeek(((dayOfWeekOfNextTime + dayPlus) % 7) + 1) && dayPlus <= 7) {
                dayPlus++
            }

            if (dayPlus == 0) {
                return WritablePair(next, null)
            } else {
                //return 0:00 of next available dayOfWeek
                return WritablePair(TimeHelper.addDays(TimeHelper.cutTimePartFromEpoch(next), dayPlus), null)
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
                    if (timePoint in (rangeStartAt until rangeStartAt + rangeLengthHour * DateUtils.HOUR_IN_MILLIS)) {
                        currentPivotStartValue = rangeStartAt
                        break
                    }
                }
            }
            return currentPivotStartValue
        }
    }
}