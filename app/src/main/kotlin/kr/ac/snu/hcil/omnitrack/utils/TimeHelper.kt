package kr.ac.snu.hcil.omnitrack.utils

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Young-Ho on 8
 * http://www.mkyong.com/java/java-time-elapsed-in-days-hours-minutes-seconds/
 **/
object TimeHelper {

    enum class Length {
        FULL, SHORT, SHORTEST
    }

    const val secondsInMilli: Long = 1000
    const val minutesInMilli = secondsInMilli * 60
    const val hoursInMilli = minutesInMilli * 60
    const val daysInMilli = hoursInMilli * 24

    val DAY_OF_WEEK_FULL_FORMAT: SimpleDateFormat by lazy { SimpleDateFormat("EEEE") }
    val DAY_OF_WEEK_SHORT_FORMAT: SimpleDateFormat by lazy { SimpleDateFormat("EEE") }


    fun addDays(timestamp: Long, days: Int): Long {
        return timestamp + days * daysInMilli
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

    fun durationToText(duration: Long, useShortUnits: Boolean, context: Context): String {
        if (duration.equals(0)) {
            return context.getString(R.string.time_duration_no_difference)
        }

        var different = duration
        val elapsedDays = different / daysInMilli
        different = different % daysInMilli

        val elapsedHours = different / hoursInMilli
        different = different % hoursInMilli

        val elapsedMinutes = different / minutesInMilli
        different = different % minutesInMilli

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


    fun compareTimePortions(c1: Calendar, c2: Calendar): Int {
        val h1 = c1.getHourOfDay()
        val h2 = c2.getHourOfDay()
        val m1 = c1.getMinute()
        val m2 = c2.getMinute()
        val s1 = c1.getMinute()
        val s2 = c2.getMinute()
        val ms1 = c1.get(Calendar.MILLISECOND)
        val ms2 = c2.get(Calendar.MILLISECOND)

        val t1 = h1*3600*1000 + m1 * 60 * 1000 + s1 * 1000 + ms1
        val t2 =  h2*3600*1000 + m2 * 60 * 1000 + s2 * 1000 + ms2

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
}