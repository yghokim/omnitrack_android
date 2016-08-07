package kr.ac.snu.hcil.omnitrack.utils

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import java.util.*

/**
 * Created by Young-Ho on 8
 * http://www.mkyong.com/java/java-time-elapsed-in-days-hours-minutes-seconds/
 **/
object TimeHelper {

    val secondsInMilli: Long = 1000
    val minutesInMilli = secondsInMilli * 60
    val hoursInMilli = minutesInMilli * 60
    val daysInMilli = hoursInMilli * 24


    fun cutTimePartFromEpoch(timestamp: Long): Long {
        val cal = Calendar.getInstance()       // get calendar instance
        cal.timeInMillis = timestamp                           // set cal to date
        cal.set(Calendar.HOUR_OF_DAY, 0)            // set hour to midnight
        cal.set(Calendar.MINUTE, 0)                 // set minute in hour
        cal.set(Calendar.SECOND, 0)                 // set second in minute
        cal.set(Calendar.MILLISECOND, 0)            // set millis in second
        return cal.timeInMillis
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
}