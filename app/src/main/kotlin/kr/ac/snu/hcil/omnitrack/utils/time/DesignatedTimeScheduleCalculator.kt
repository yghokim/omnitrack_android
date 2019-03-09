package kr.ac.snu.hcil.omnitrack.utils.time

import com.google.gson.JsonObject
import kr.ac.snu.hcil.android.common.containers.WritablePair
import kr.ac.snu.hcil.android.common.time.getDayOfWeek
import java.util.*

/**
 * Created by younghokim on 2017-06-01.
 */
class DesignatedTimeScheduleCalculator : TimeScheduleCalculator() {

    private var alarmHourOfDay: Int = 0
    private var alarmMinute: Int = 0
    private var alarmSecond: Int = 0

    fun setAlarmTime(hourOfDay: Int = 0, minute: Int = 0, second: Int = 0): DesignatedTimeScheduleCalculator {
        this.alarmHourOfDay = hourOfDay
        this.alarmMinute = minute
        this.alarmSecond = second
        return this
    }

    override fun calculateInfiniteNextTime(last: Long?, now: Long): WritablePair<Long, JsonObject?>? {
        val cacheCalendar = GregorianCalendar.getInstance()
        cacheCalendar.timeInMillis = now
        cacheCalendar.set(Calendar.HOUR_OF_DAY, alarmHourOfDay)
        cacheCalendar.set(Calendar.MINUTE, alarmMinute)
        cacheCalendar.set(Calendar.SECOND, alarmSecond)
        cacheCalendar.set(Calendar.MILLISECOND, 0)

        cacheCalendar.add(Calendar.DAY_OF_YEAR, -1)
        for (i in 0..7) {
            cacheCalendar.add(Calendar.DAY_OF_YEAR, 1)
            val dow = cacheCalendar.getDayOfWeek()
            if (isAvailableDayOfWeek(dow) && cacheCalendar.timeInMillis >= now) {
                return WritablePair(cacheCalendar.timeInMillis, null)
            }
        }

        return null
    }

}