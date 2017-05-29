package kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales

import android.text.format.DateUtils
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.visualization.Granularity
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.IAxisScale
import kr.ac.snu.hcil.omnitrack.utils.TimeHelper
import kr.ac.snu.hcil.omnitrack.utils.getHourOfDay
import java.util.*

/**
 * Created by Young-Ho Kim on 16. 9. 8
 */
class TimeLinearScale : IAxisScale<Long> {

    private var rangeFrom: Float = 0f
    private var rangeTo: Float = 0f

    private var domainDataMin: Long = 0L
    private var domainDataMax: Long = 0L


    private var domainExtendedMin: Long = 0L
    private var domainExtendedMax: Long = 0L

    private var domainInterval: Long = 0L

    override var tickFormat : IAxisScale.ITickFormat<Long>? = null

    val tsCache = TimeSpan()

    private val calendarCache = Calendar.getInstance()


    private var _numIntervals: Int = 5

    fun setDomain(min: Long, max: Long): TimeLinearScale {
        domainDataMin = min
        domainDataMax = max
        domainExtendedMin = min
        domainExtendedMax = max

        return this
    }

    fun setTicksBasedOnGranularity(granularity: Granularity): TimeLinearScale {

        when(granularity)
        {
            Granularity.DAY ->//time
            {
                //3 hours
                _numIntervals = 8
                domainInterval = 3 * DateUtils.HOUR_IN_MILLIS
                tickFormat = object: IAxisScale.ITickFormat<Long> {
                    override fun format(value: Long, index: Int): String {
                        calendarCache.timeInMillis = value
                        val hourOfDay = calendarCache.getHourOfDay()

                        return if (hourOfDay == 12) {
                            OTApplication.app.resourcesWrapped.getString(R.string.msg_noon)
                        } else if (hourOfDay == 0 || hourOfDay == 23) {
                            TimeHelper.FORMAT_DAY.format(Date(value))
                        } else {
                            "haha"
                        }
                    }
                }
            }

            Granularity.WEEK ->
            {
                // day of week
                _numIntervals = 7
                domainInterval = DateUtils.DAY_IN_MILLIS
            }

            Granularity.MONTH ->
            {
                //5 days

            }

            Granularity.YEAR ->
            {
                //month
                _numIntervals = 12
                domainInterval = DateUtils.DAY_IN_MILLIS * 30
            }

            else -> {
            }
        }

        return this
    }

    fun setScopeDomain(pivot: Long, granularity: Granularity) {
        granularity.convertToRange(pivot, tsCache)
        domainExtendedMin = tsCache.from
        domainExtendedMax = tsCache.to
    }

    override fun getTickCoordAt(index: Int): Float {
        return this[domainExtendedMin + domainInterval * index]
    }

    override fun getTickInterval(): Float {
        return (rangeTo - rangeFrom) / _numIntervals
    }

    override fun getTickLabelAt(index: Int): String {
        val tickValue = domainExtendedMin + domainInterval * index
        return tickFormat?.format(tickValue, index) ?: tickValue.toString()
    }

    override fun get(domain: Long): Float {
        return rangeFrom + (rangeTo - rangeFrom) * ((domain - domainExtendedMin).toDouble() / (domainExtendedMax - domainExtendedMin)).toFloat()
    }


    override val numTicks: Int get() = _numIntervals + 1

    override fun setRealCoordRange(from: Float, to: Float): TimeLinearScale {
        rangeFrom = from
        rangeTo = to

        return this
    }


}