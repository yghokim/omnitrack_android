package kr.ac.snu.hcil.omnitrack.utils.time

import android.content.Context
import android.text.format.DateUtils
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Young-Ho on 8
 * http://www.mkyong.com/java/java-time-elapsed-in-days-hours-minutes-seconds/
 **/
object TimeHelper {

    const val secondsInMilli: Long = 1000
    const val minutesInMilli = secondsInMilli * 60
    const val hoursInMilli = minutesInMilli * 60
    const val daysInMilli = hoursInMilli * 24

    enum class Length {
        FULL, SHORT, SHORTEST
    }

    enum class DayOfWeek {
        Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday
    }


    val DAY_OF_WEEK_FULL_FORMAT: SimpleDateFormat by lazy { SimpleDateFormat("EEEE", Locale.getDefault()) }
    val DAY_OF_WEEK_SHORT_FORMAT: SimpleDateFormat by lazy { SimpleDateFormat("EEE", Locale.getDefault()) }

    val FORMAT_ISO_8601: SimpleDateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT)
    }

    val FORMAT_YYYY_MM_DD: SimpleDateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
    }

    fun addDays(timestamp: Long, days: Int): Long {
        return timestamp + days * daysInMilli
    }

    fun getTodayRange(now: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MINUTE, 0)

        cal.set(Calendar.HOUR_OF_DAY, 0)

        val start = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1)

        val end = cal.timeInMillis

        return Pair(start, end)
    }

    fun isSameDay(timeStampA: Long, timeStampB: Long, timeZone: TimeZone? = null): Boolean {
        val cal = Calendar.getInstance()       // get calendar instance
        timeZone?.let { cal.timeZone = timeZone }
        cal.timeInMillis = timeStampA
        val yearA = cal.getYear()
        val dayOfYearA = cal.get(Calendar.DAY_OF_YEAR)
        cal.timeInMillis = timeStampB
        val yearB = cal.getYear()
        val dayOfYearB = cal.get(Calendar.DAY_OF_YEAR)
        println("$yearA-$dayOfYearA, $yearB-$dayOfYearB")
        return yearA == yearB && dayOfYearA == dayOfYearB
    }

    fun cutTimePartFromEpoch(timestamp: Long): Long {
        val cal = Calendar.getInstance()       // get calendar instance
        cal.timeInMillis = timestamp                           // set cal to date
        cal.set(Calendar.HOUR_OF_DAY, 0)            // set hour to midnight
        cal.set(Calendar.MINUTE, 0)                 // set minute in hour
        cal.set(Calendar.SECOND, 0)                 // set second in minute
        cal.set(Calendar.MILLISECOND, 0)            // set millis in second
        return cal.timeInMillis
    }

    fun cutMillisecond(timestamp: Long): Long {
        return timestamp / 1000 * 1000
    }

    fun sliceToDate(start: Long, end: Long): List<Pair<Date, Date>> {
        val points: Array<Long?>

        val startCal = Calendar.getInstance()
        startCal.timeInMillis = start

        val dateDiff = (end / DateUtils.DAY_IN_MILLIS).toInt() - (start / DateUtils.DAY_IN_MILLIS).toInt()

        if (dateDiff <= 0) {
            points = arrayOf(start, end)
        } else {
            points = arrayOfNulls(if (end % DateUtils.DAY_IN_MILLIS == 0L) {
                (2 + dateDiff - 1)
            } else {
                2 + dateDiff
            })

            points[0] = start

            startCal.setHourOfDay(0, true)
            for (i in 1..dateDiff) {
                points[i] = startCal.timeInMillis + i * DateUtils.DAY_IN_MILLIS
            }

            if (end % DateUtils.DAY_IN_MILLIS != 0L) {
                points[points.size - 1] = end
            }
        }

        val list = ArrayList<Pair<Date, Date>>()

        var startDate: Date
        var endDate: Date
        for (i in 0..(points.size - 2)) {
            startDate = Date(points[i]!!)
            endDate = if (i + 1 < points.size - 1) {
                Date(points[i + 1]!! - DateUtils.MINUTE_IN_MILLIS)
            } else {
                Date(points[i + 1]!!)
            }

            list.add(Pair(startDate, endDate))
        }

        return list
    }

    fun getDateText(timestamp: Long, context: Context): String {
        val cal = Calendar.getInstance()

        cal.timeInMillis = timestamp
        if (DateUtils.isToday(timestamp)) {
            return context.resources.getString(R.string.msg_today)
        } else {
            val yesterdayCal = Calendar.getInstance()
            yesterdayCal.add(Calendar.DAY_OF_YEAR, -1)
            if (yesterdayCal.getYear() == cal.getYear() && yesterdayCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)) {
                return context.resources.getString(R.string.msg_yesterday)
            } else {
                return SimpleDateFormat(context.resources.getString(R.string.msg_tracker_list_date_format)).format(cal.time)
            }
        }
    }

    fun durationToText(duration: Long, useShortUnits: Boolean, context: Context): String {
        if (duration == 0L) {
            return context.getString(R.string.time_duration_no_difference)
        }

        var different = duration
        val elapsedDays = different / daysInMilli
        different %= daysInMilli

        val elapsedHours = different / hoursInMilli
        different %= hoursInMilli

        val elapsedMinutes = different / minutesInMilli
        different %= minutesInMilli

        val elapsedSeconds = different / secondsInMilli


        val builder = StringBuilder()

        if (elapsedDays > 0) {
            builder.append(' ', context.resources.getQuantityString(if (useShortUnits) {
                R.plurals.time_duration_day
            } else {
                R.plurals.time_duration_day
            }, elapsedDays.toInt(), elapsedDays))
        }

        if (elapsedHours > 0) {
            builder.append(' ', context.resources.getQuantityString(if (useShortUnits) {
                R.plurals.time_duration_hour_short
            } else {
                R.plurals.time_duration_hour
            }, elapsedHours.toInt(), elapsedHours))
        }

        if (elapsedMinutes > 0) {
            builder.append(' ', context.resources.getQuantityString(if (useShortUnits) {
                R.plurals.time_duration_minute_short
            } else {
                R.plurals.time_duration_minute
            }, elapsedMinutes.toInt(), elapsedMinutes))
        }

        if (elapsedSeconds > 0) {
            builder.append(' ', context.resources.getQuantityString(if (useShortUnits) {
                R.plurals.time_duration_second_short
            } else {
                R.plurals.time_duration_second
            }, elapsedSeconds.toInt(), elapsedSeconds))
        }

        return builder.trim().toString()
    }

    fun getYear(time: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = time
        return cal.getYear()
    }


    fun compareTimePortions(c1: Calendar, c2: Calendar): Int {
        val h1 = c1.getHourOfDay()
        val h2 = c2.getHourOfDay()
        val m1 = c1.getMinute()
        val m2 = c2.getMinute()
        val s1 = c1.getMinute()
        val s2 = c2.getMinute()
        val ms1 = c1.get(Calendar.MILLISECOND)
        val ms2 = c2.get(Calendar.MILLISECOND)

        val t1 = h1 * 3600 * 1000 + m1 * 60 * 1000 + s1 * 1000 + ms1
        val t2 = h2 * 3600 * 1000 + m2 * 60 * 1000 + s2 * 1000 + ms2

        return t1 - t2
    }

    fun getDaysLeftToClosestDayOfWeek(pivot: Calendar, dayOfWeek: Int): Int {

        return (dayOfWeek + 7 - pivot.getDayOfWeek()) % 7
    }

    fun getDayOfWeekName(date: Date, length: Length): CharSequence {
        return when (length) {
            Length.FULL -> DAY_OF_WEEK_FULL_FORMAT.format(date)
            Length.SHORT -> DAY_OF_WEEK_SHORT_FORMAT.format(date)
            Length.SHORTEST -> DAY_OF_WEEK_SHORT_FORMAT.format(date)
        }
    }

    fun getDayOfWeekBooleanInFlag(dayOfWeek: Int, flags: Int): Boolean {
        return BitwiseOperationHelper.getBooleanAt(flags, 6 - dayOfWeek)
    }

    fun loopForDays(from: Long, to: Long, loopHandler: (time: Long, start: Long, end: Long, dayOfYear: Int) -> Unit) {
        val rangeCal = Calendar.getInstance()
        val cal = Calendar.getInstance()
        cal.timeInMillis = from

        while (cal.timeInMillis <= to) {
            rangeCal.timeInMillis = cal.timeInMillis
            rangeCal.setHourOfDay(0, true)

            loopHandler(cal.timeInMillis, rangeCal.timeInMillis, rangeCal.timeInMillis + DateUtils.DAY_IN_MILLIS, cal.get(Calendar.DAY_OF_YEAR))

            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }


    fun roundToSeconds(time: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = time
        val millis = cal.get(Calendar.MILLISECOND)
        cal.set(Calendar.MILLISECOND, 0)
        if (millis >= 500) {
            cal.add(Calendar.SECOND, 1)
        }

        return cal.timeInMillis
    }
}