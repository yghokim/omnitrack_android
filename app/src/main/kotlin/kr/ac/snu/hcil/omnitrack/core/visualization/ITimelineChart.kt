package kr.ac.snu.hcil.omnitrack.core.visualization

import android.text.format.DateUtils
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-08.
 */
interface ITimelineChart {
    enum class Granularity(val nameId: Int) {
        DAY(R.string.granularity_day) {
            override fun convertToRange(time: Long, out: TimeSpan) {
                val cal = GregorianCalendar.getInstance()
                cal.timeInMillis = time
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                out.from = cal.timeInMillis
                out.duration = DateUtils.DAY_IN_MILLIS.toInt()
            }

        },
        WEEK(R.string.granularity_week) {
            override fun convertToRange(time: Long, out: TimeSpan) {
                DAY.convertToRange(time, out)

                val cal = GregorianCalendar.getInstance()
                cal.timeInMillis = out.from
                cal.set(Calendar.DAY_OF_WEEK, 1)

                out.from = cal.timeInMillis
                out.duration = DateUtils.WEEK_IN_MILLIS.toInt()
            }
        },
        MONTH(R.string.granularity_month) {
            override fun convertToRange(time: Long, out: TimeSpan) {
                val cal = GregorianCalendar.getInstance()
                cal.timeInMillis = time
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                out.from = cal.timeInMillis
                out.duration = (cal.getMaximum(Calendar.DAY_OF_MONTH) * DateUtils.DAY_IN_MILLIS).toInt()
            }
        },
        YEAR(R.string.granularity_year) {
            override fun convertToRange(time: Long, out: TimeSpan) {
                val cal = GregorianCalendar.getInstance()
                cal.timeInMillis = time
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                out.from = cal.timeInMillis
                out.duration = DateUtils.YEAR_IN_MILLIS.toInt()
            }
        };

        abstract fun convertToRange(time: Long, out: TimeSpan)
    }

    val isScopeControlSupported: Boolean

    fun setTimeScope(time: Long, scope: Granularity)

    fun getTimeScope(): TimeSpan


}