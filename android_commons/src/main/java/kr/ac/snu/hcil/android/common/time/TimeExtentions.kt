package kr.ac.snu.hcil.android.common.time

import java.util.*

fun Calendar.getYear(): Int {
    return get(Calendar.YEAR)
}

fun Calendar.getZeroBasedMonth(): Int {
    return get(Calendar.MONTH)
}

fun Calendar.getDayOfMonth(): Int {
    return get(Calendar.DAY_OF_MONTH)
}

fun Calendar.getDayOfWeek(): Int {
    return get(Calendar.DAY_OF_WEEK)
}

fun Calendar.getHour(): Int {
    return get(Calendar.HOUR)
}

fun Calendar.getHourOfDay(): Int {
    return get(Calendar.HOUR_OF_DAY)
}

fun Calendar.getMinute(): Int {
    return get(Calendar.MINUTE)
}

fun Calendar.getSecond(): Int {
    return get(Calendar.SECOND)
}

fun Calendar.getAmPm(): Int {
    return get(Calendar.AM_PM)
}

fun Calendar.setHourOfDay(hour: Int, cutUnder: Boolean = false) {
    set(Calendar.HOUR_OF_DAY, hour)
    if (cutUnder) {
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}