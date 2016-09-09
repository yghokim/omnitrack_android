package kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales

import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.IAxisScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.NiceNumberHelper

/**
 * Created by Young-Ho on 9/8/2016.
 */
class NumericScale: IAxisScale<Float> {

    private var rangeFrom: Float = 0f
    private var rangeTo: Float = 0f

    private val niceScale = NiceNumberHelper()

    private var domainExtendedMin: Float = 0f
    private var domainExtendedMax: Float = 0f

    private var domainDataMin: Float = 0f
    private var domainDataMax: Float = 0f

    private var tickSpacingInDomain: Float = 0f

    private var _numTicks: Int = 0


    override var tickFormat: IAxisScale.ITickFormat<Float>? = null

    override fun setRealCoordRange(from: Float, to: Float): NumericScale {
        this.rangeFrom = from
        this.rangeTo = to

        return this
    }

    fun setDomain(from: Float, to: Float, isZeroBased: Boolean): NumericScale
    {
        domainDataMax = to
        domainDataMin = if (isZeroBased) {
            Math.min(0f, from)
        } else from

        domainExtendedMax = to
        domainExtendedMin = from

        _numTicks = 5
        tickSpacingInDomain = (domainDataMax - domainDataMin) / _numTicks

        return this
    }

    fun nice(isInteger: Boolean): NumericScale {

        niceScale.calculate(domainDataMin, domainDataMax, isInteger)
        domainExtendedMin = niceScale.niceMin.toFloat()

        domainExtendedMax = niceScale.niceMax.toFloat()
        tickSpacingInDomain = niceScale.niceTickSpacing.toFloat()
        _numTicks = ((domainExtendedMax - domainExtendedMin) / tickSpacingInDomain).toInt() + 1

        println("nice min: $domainExtendedMin, max: $domainExtendedMax, numTicks: $_numTicks")

        return this
    }


    override val numTicks: Int get() = _numTicks

    fun getTickDomainAt(index: Int): Float{
        return domainExtendedMin + index * tickSpacingInDomain
    }

    override fun getTickCoordAt(index: Int): Float {
        return this[getTickDomainAt(index)]
    }

    override fun getTickLabelAt(index: Int): String {
        return tickFormat?.format(getTickDomainAt(index), index) ?: getTickDomainAt(index).toString()
    }

    override fun getTickInterval(): Float {
        return tickSpacingInDomain * (rangeTo - rangeFrom) / (domainExtendedMax - domainExtendedMin)
    }


    override fun get(domain: Float): Float {
        val converted = rangeFrom + (rangeTo - rangeFrom) * (domain - domainExtendedMin) / (domainExtendedMax - domainExtendedMin)

        println("from ${domain} to $converted, rangeFrom:${rangeFrom}, rangeTo: $rangeTo, domainMin: $domainExtendedMin, domainMax: $domainExtendedMax")

        return converted
    }

}