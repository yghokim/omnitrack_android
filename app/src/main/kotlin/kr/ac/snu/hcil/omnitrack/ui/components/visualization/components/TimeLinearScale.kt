package kr.ac.snu.hcil.omnitrack.ui.components.visualization.components

import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.visualization.ITimelineChart

/**
 * Created by younghokim on 16. 9. 8..
 */
class TimeLinearScale : IAxisScale {


    private var rangeFrom: Float = 0f
    private var rangeTo: Float = 0f

    private var domainDataMin: Long = 0L
    private var domainDataMax: Long = 0L


    private var domainExtendedMin: Long = 0L
    private var domainExtendedMax: Long = 0L

    private var domainInterval: Long = 0L

    val tsCache = TimeSpan()


    private var _numIntervals: Int = 5

    fun setDomain(min: Long, max: Long): TimeLinearScale {
        domainDataMin = min
        domainDataMax = max
        domainExtendedMin = min
        domainExtendedMax = max

        return this
    }

    fun setScopeDomain(pivot: Long, granularity: ITimelineChart.Granularity) {
        granularity.convertToRange(pivot, tsCache)
        domainExtendedMin = tsCache.from
        domainExtendedMax = tsCache.to
    }

    override fun getTickCoordAt(index: Int): Float {
        return convertDomainToRangeScale(domainExtendedMin + domainInterval * index)
    }

    override fun getTickInterval(): Float {
        return (rangeTo - rangeFrom) / _numIntervals
    }

    override fun getTickLabelAt(index: Int): String {
        val tickValue = domainExtendedMin + domainInterval * index
        return tickValue.toString()
    }

    fun convertDomainToRangeScale(domainValue: Long): Float {
        return rangeFrom + (rangeTo - rangeFrom) * ((domainValue - domainExtendedMin).toDouble() / (domainExtendedMax - domainExtendedMin)).toFloat()
    }

    override val numTicks: Int get() = _numIntervals + 1

    override fun setRealCoordRange(from: Float, to: Float): TimeLinearScale {
        rangeFrom = from
        rangeTo = to

        return this
    }


}