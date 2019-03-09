package kr.ac.snu.hcil.android.common.time

import java.util.*

/**
 * Created by younghokim on 2017. 6. 5..
 */
data class Time(val hourOfDay: Int, val minute: Int, val second: Int) {

    val hour: Int get() = if (hourOfDay > 12) (hourOfDay - 12) else hourOfDay
    val amPm: Int get() = if (hourOfDay >= 12) Calendar.PM else Calendar.AM
}