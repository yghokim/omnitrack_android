package kr.ac.snu.hcil.omnitrack.utils.time

import kr.ac.snu.hcil.omnitrack.utils.getDayOfWeek
import java.util.*

/**
 * Created by Young-Ho on 5/31/2017.
 */
class IntervalTimeScheduleCalculator : TimeScheduleCalculator() {

    var intervalMillis: Long = 0

    var fromHourOfDay: Int = 0
    var toHourOfDay: Int = 24

    val cacheCalendar = GregorianCalendar.getInstance()

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
            //TODO

        } else {
            val next = getNearestFutureNextIntervalTime(realPivot, now, intervalMillis)
            cacheCalendar.timeInMillis = next
            val dayOfWeekOfNextTime = cacheCalendar.getDayOfWeek()
            var dayPlus = 0
            while (availableDaysOfWeek[(dayOfWeekOfNextTime + dayPlus) % 7] == false) {
                dayPlus++
            }

            if (dayPlus == 0) {
                return next
            } else {
                //return 0:00 of next available dayOfWeek
                return TimeHelper.addDays(TimeHelper.cutTimePartFromEpoch(next), dayPlus)
            }
        }

        return 0L
    }

/*
        val intervalMillis = IntervalConfig.getIntervalSeconds(configVariables) * 1000
        val startBoundHourOfDay = IntervalConfig.getStartHour(configVariables)
        val endBoundHourOfDay = IntervalConfig.getEndHour(configVariables)


        val isRangeBounded = startBoundHourOfDay != endBoundHourOfDay

        val realPivot = if (pivot == TRIGGER_TIME_NEVER_TRIGGERED) {
            now
        } else pivot

        val intrinsicNextTime =
                if (!isRangeBounded) {
                    if (isRepeated) {
                        cacheCalendar.timeInMillis = now
                        if (Range.isDayOfWeekUsed(rangeVariables, cacheCalendar.getDayOfWeek())) {
                            //yesterday is used
                            getIntrinsicNextIntervalTime(realPivot, now, intervalMillis)
                        } else {
                            getClosestLowerBoundOfInterval(now, startBoundHourOfDay)
                        }
                    } else {
                        cacheCalendar.timeInMillis = now
                        cacheCalendar.setHourOfDay(0, true)

                        val intrinsic = getIntrinsicNextIntervalTime(realPivot, now, intervalMillis)
                        if (intrinsic < cacheCalendar.timeInMillis + DateUtils.DAY_IN_MILLIS) {
                            intrinsic
                        } else {
                            if (pivot == TRIGGER_TIME_NEVER_TRIGGERED) {
                                cacheCalendar.timeInMillis + DateUtils.DAY_IN_MILLIS
                            } else 0L
                        }
                    }
                } else {
                    //extract hour range of today
                    cacheCalendar.timeInMillis = now

                    cacheCalendar.setHourOfDay(startBoundHourOfDay, cutUnder = true)
                    val lowerBoundToday = cacheCalendar.timeInMillis

                    cacheCalendar.setHourOfDay(endBoundHourOfDay, cutUnder = true)
                    if (startBoundHourOfDay >= endBoundHourOfDay) {
                        cacheCalendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    val upperBoundToday = cacheCalendar.timeInMillis

                    val upperBoundLastDay = TimeHelper.addDays(upperBoundToday, -1)

                    if (now < upperBoundLastDay) {
                        if (isRepeated) {
                            cacheCalendar.timeInMillis = now
                            cacheCalendar.add(Calendar.DAY_OF_YEAR, -1)
                            if (Range.isDayOfWeekUsed(rangeVariables, cacheCalendar.getDayOfWeek())) {
                                //yesterday is used
                                val intrinsic = getIntrinsicNextIntervalTime(realPivot, now, intervalMillis)
                                if (intrinsic < upperBoundLastDay) {
                                    intrinsic
                                } else {
                                    getClosestLowerBoundOfInterval(now, startBoundHourOfDay)
                                }
                            } else {
                                getClosestLowerBoundOfInterval(now, startBoundHourOfDay)
                            }
                        } else {

                            val intrinsic = getIntrinsicNextIntervalTime(realPivot, now, intervalMillis)
                            if (intrinsic < upperBoundLastDay) {
                                intrinsic
                            } else {
                                if (pivot == TRIGGER_TIME_NEVER_TRIGGERED) {
                                    lowerBoundToday
                                } else 0L
                            }
                        }
                    } else if (upperBoundLastDay <= now && now < lowerBoundToday) {
                        if (isRepeated) {
                            getClosestLowerBoundOfInterval(now, startBoundHourOfDay)
                        } else {
                            if (pivot == TRIGGER_TIME_NEVER_TRIGGERED) {
                                lowerBoundToday
                            } else {
                                0L
                            }
                        }
                    } else if (lowerBoundToday <= now && now < upperBoundToday) {
                        //now is later than upperBound
                        if (isRepeated) {
                            cacheCalendar.timeInMillis = now
                            if (Range.isDayOfWeekUsed(rangeVariables, cacheCalendar.getDayOfWeek())) {
                                //yesterday is used
                                val intrinsic = getIntrinsicNextIntervalTime(realPivot, now, intervalMillis)
                                if (intrinsic < upperBoundToday) {
                                    intrinsic
                                } else {
                                    getClosestLowerBoundOfInterval(now, startBoundHourOfDay)
                                }
                            } else {
                                getClosestLowerBoundOfInterval(now, startBoundHourOfDay)
                            }
                        } else {
                            val intrinsic = getIntrinsicNextIntervalTime(realPivot, now, intervalMillis)
                            if (intrinsic < upperBoundToday) {
                                intrinsic
                            } else {
                                if (pivot == TRIGGER_TIME_NEVER_TRIGGERED) {
                                    TimeHelper.addDays(lowerBoundToday, 1)
                                } else 0L
                            }
                        }
                    } else {
                        if (isRepeated) {
                            getClosestLowerBoundOfInterval(now, startBoundHourOfDay)
                        } else {
                            if (pivot == TRIGGER_TIME_NEVER_TRIGGERED) {
                                TimeHelper.addDays(lowerBoundToday, 1)
                            } else {
                                0L
                            }
                        }
                    }
                }

        intrinsicNextTime*/


}