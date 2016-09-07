package kr.ac.snu.hcil.omnitrack.ui.components.visualization.components

/**
 * Created by Young-Ho on 9/8/2016.
 */
class NumericScale: IAxisScale {

    private var rangeFrom: Float = 0f
    private var rangeTo: Float = 0f

    private val niceScale = NiceScale()

    private var domainMin: Float = 0f
    private var domainMax: Float = 0f
    private var tickSpacingInDomain: Float = 0f

    private var _numTicks: Int = 0

    var tickFormat : ((Float)->String)? = null

    override fun setRealCoordRange(from: Float, to: Float) {
        this.rangeFrom = from
        this.rangeTo = to
    }

    fun setDomain(from: Float, to: Float)
    {
        niceScale.setMinMaxPoints(from.toDouble(), to.toDouble())
        domainMin = niceScale.niceMin.toFloat()
        domainMax = niceScale.niceMax.toFloat()
        tickSpacingInDomain = niceScale.tickSpacing.toFloat()
        _numTicks = ((domainMax - domainMin)/tickSpacingInDomain + 1f).toInt()
    }

    override val numTicks: Int get() = _numTicks

    fun getTickDomainAt(index: Int): Float{
        return domainMin + index * tickSpacingInDomain
    }

    override fun getTickCoordAt(index: Int): Float {
        return convertDomainToRangeScale(getTickDomainAt(index))
    }

    fun convertDomainToRangeScale(domainValue: Float): Float{
        return rangeFrom + (rangeTo - rangeFrom) * (domainValue - domainMin)/(domainMax - domainMin)
    }

    override fun getTickLabelAt(index: Int): String {
        return tickFormat?.invoke(getTickDomainAt(index)) ?: getTickLabelAt(index).toString()
    }

    override fun getTickInterval(): Float {
        return tickSpacingInDomain * (rangeTo - rangeFrom)/(domainMax - domainMin)
    }

}