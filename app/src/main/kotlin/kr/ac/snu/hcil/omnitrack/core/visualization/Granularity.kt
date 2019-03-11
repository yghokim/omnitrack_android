package kr.ac.snu.hcil.omnitrack.core.visualization

import android.content.Context
import android.text.format.DateUtils
import kr.ac.snu.hcil.android.common.time.TimeHelper
import kr.ac.snu.hcil.android.common.time.getYear
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.types.TimeSpan
import kr.ac.snu.hcil.omnitrack.utils.time.LocalTimeFormats
import java.util.*

/**
 * Created by Young-Ho on 9/9/2016.
 */

enum class Granularity(val nameId: Int) {

    DAY(R.string.granularity_day) {
        override fun convertToRange(time: Long, out: TimeSpan) {
            val cal = GregorianCalendar.getInstance()
            cal.timeInMillis = time
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.HOUR, 0)
            cal.set(Calendar.AM_PM, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            out.from = cal.timeInMillis
            out.duration = DateUtils.DAY_IN_MILLIS
        }

        override fun getIntervalMillis(directionToNext: Boolean, pivot: Long): Long {
            return DateUtils.DAY_IN_MILLIS
        }

        override fun getFormattedCurrentScope(time: Long, context: Context): String {
            return TimeHelper.getDateText(time, context)
        }

    },

    WEEK(R.string.granularity_week) {
        override fun convertToRange(time: Long, out: TimeSpan) {
            DAY.convertToRange(time, out)

            val cal = GregorianCalendar.getInstance()
            cal.timeInMillis = out.from
            cal.set(Calendar.DAY_OF_WEEK, 1)

            out.from = cal.timeInMillis
            out.duration = DateUtils.WEEK_IN_MILLIS
        }

        override fun getIntervalMillis(directionToNext: Boolean, pivot: Long): Long {
            return DateUtils.WEEK_IN_MILLIS
        }

        override fun getFormattedCurrentScope(time: Long, context: Context): String {
            val ts = TimeSpan()
            convertToRange(time, ts)
            val formats = getFormats(context)
            return "${formats.FORMAT_DAY_WITHOUT_YEAR.format(ts.from)} ~ ${formats.FORMAT_DAY_WITHOUT_YEAR.format(ts.to - 1)} "
        }

    },

    WEEK_2(R.string.granularity_week_2) {
        override fun convertToRange(time: Long, out: TimeSpan) {
            DAY.convertToRange(time, out)

            val cal = GregorianCalendar.getInstance()
            cal.timeInMillis = out.from
            cal.set(Calendar.DAY_OF_WEEK, 1)

            out.from = cal.timeInMillis
            out.duration = DateUtils.WEEK_IN_MILLIS * 2
        }

        override fun getIntervalMillis(directionToNext: Boolean, pivot: Long): Long {
            return 2 * DateUtils.WEEK_IN_MILLIS
        }

        override fun getFormattedCurrentScope(time: Long, context: Context): String {
            val ts = TimeSpan()
            convertToRange(time, ts)
            val formats = getFormats(context)
            return "${formats.FORMAT_DAY_WITHOUT_YEAR.format(ts.from)} ~ ${formats.FORMAT_DAY_WITHOUT_YEAR.format(ts.to - 1)} "
        }

    },

    MONTH(R.string.granularity_month) {
        override fun convertToRange(time: Long, out: TimeSpan) {
            val cal = GregorianCalendar.getInstance()
            cal.timeInMillis = time
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR, 0)
            cal.set(Calendar.AM_PM, 0)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            out.from = cal.timeInMillis

            cal.add(Calendar.MONTH, 1)
            out.duration = cal.timeInMillis - out.from
        }

        override fun getIntervalMillis(directionToNext: Boolean, pivot: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = pivot
            cal.add(Calendar.MONTH, if (directionToNext) 1 else -1)

            /*
            val pivotZeroBasedMonth = cal.getZeroBasedMonth()
            val pivotDayOfMonth = cal.getDayOfMonth()

            val monthToMove = if(directionToNext)
            {
                (pivotZeroBasedMonth + 1)%12
            }
            else{
                if(pivotZeroBasedMonth==0) 11
                else pivotZeroBasedMonth-1
            }

            if(cal.getMaxi)
            */


            return if (directionToNext) {
                cal.timeInMillis - pivot
            } else {
                pivot - cal.timeInMillis
            }
        }

        override fun getFormattedCurrentScope(time: Long, context: Context): String {

            val formats = getFormats(context)
            return formats.FORMAT_MONTH.format(Date(time))
        }

    },

    YEAR(R.string.granularity_year) {
        override fun convertToRange(time: Long, out: TimeSpan) {
            val cal = GregorianCalendar.getInstance()
            cal.timeInMillis = time
            cal.set(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.HOUR, 0)
            cal.set(Calendar.AM_PM, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            out.from = cal.timeInMillis
            out.duration = DateUtils.YEAR_IN_MILLIS
        }

        override fun getIntervalMillis(directionToNext: Boolean, pivot: Long): Long {
            return DateUtils.YEAR_IN_MILLIS
        }

        override fun getFormattedCurrentScope(time: Long, context: Context): String {
            val cal = Calendar.getInstance()
            cal.timeInMillis = time
            return cal.getYear().toString()
        }


    },


    WEEK_REL(R.string.granularity_week) {
        override fun convertToRange(time: Long, out: TimeSpan) {
            DAY.convertToRange(time, out)

            out.from = out.to - DateUtils.WEEK_IN_MILLIS
            out.duration = DateUtils.WEEK_IN_MILLIS
        }

        override fun getIntervalMillis(directionToNext: Boolean, pivot: Long): Long {
            return DateUtils.WEEK_IN_MILLIS
        }

        override fun getFormattedCurrentScope(time: Long, context: Context): String {
            val ts = TimeSpan()
            convertToRange(time, ts)

            val formats = getFormats(context)
            return "${formats.FORMAT_DAY_WITHOUT_YEAR.format(ts.from)} ~ ${formats.FORMAT_DAY_WITHOUT_YEAR.format(ts.to - 1)} "
        }

    },

    WEEK_2_REL(R.string.granularity_week_2) {
        override fun convertToRange(time: Long, out: TimeSpan) {
            DAY.convertToRange(time, out)
            out.from = out.to - DateUtils.WEEK_IN_MILLIS * 2
            out.duration = DateUtils.WEEK_IN_MILLIS * 2
        }

        override fun getIntervalMillis(directionToNext: Boolean, pivot: Long): Long {
            return 2 * DateUtils.WEEK_IN_MILLIS
        }

        override fun getFormattedCurrentScope(time: Long, context: Context): String {
            val ts = TimeSpan()
            convertToRange(time, ts)

            val formats = getFormats(context)
            return "${formats.FORMAT_DAY_WITHOUT_YEAR.format(ts.from)} ~ ${formats.FORMAT_DAY_WITHOUT_YEAR.format(ts.to - 1)} "
        }

    },

    WEEK_4_REL(R.string.granularity_week_4) {
        override fun convertToRange(time: Long, out: TimeSpan) {
            DAY.convertToRange(time, out)
            out.from = out.to - DateUtils.WEEK_IN_MILLIS * 4
            out.duration = DateUtils.WEEK_IN_MILLIS * 4
        }

        override fun getIntervalMillis(directionToNext: Boolean, pivot: Long): Long {
            return 4 * DateUtils.WEEK_IN_MILLIS
        }

        override fun getFormattedCurrentScope(time: Long, context: Context): String {
            val ts = TimeSpan()
            convertToRange(time, ts)

            val formats = getFormats(context)
            return "${formats.FORMAT_DAY_WITHOUT_YEAR.format(ts.from)} ~ ${formats.FORMAT_DAY_WITHOUT_YEAR.format(ts.to - 1)} "
        }

    };

    abstract fun convertToRange(time: Long, out: TimeSpan)
    abstract fun getIntervalMillis(directionToNext: Boolean, pivot: Long): Long

    abstract fun getFormattedCurrentScope(time: Long, context: Context): String

    protected fun getFormats(context: Context): LocalTimeFormats {
        return (context.applicationContext as OTAndroidApp).applicationComponent.getLocalTimeFormats()
    }
}