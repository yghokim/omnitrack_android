package kr.ac.snu.hcil.omnitrack.ui.components.visualization.components

/**
 * Created by Young-Ho on 9/8/2016.
 */
class NiceScale
/**
 * Instantiates a new instance of the NiceScale class.
    http://stackoverflow.com/questions/8506881/nice-label-algorithm-for-charts-with-minimum-ticks
    Author: Incongruous
 * @param min the minimum data point on the axis
 * *
 * @param max the maximum data point on the axis
 */
() {

    var minPoint: Double =0.0
    var maxPoint: Double = 0.0

    private var maxTicks = 5.0
    var tickSpacing: Double = 0.toDouble()
    private set

    private var range: Double = 0.toDouble()
    var niceMin: Double = 0.toDouble()
    private set

    var niceMax: Double = 0.toDouble()
    private set

    init {
    }

    /**
     * Calculate and update values for tick spacing and nice
     * minimum and maximum data points on the axis.
     */
    fun calculate() {

        this.range = niceNum(maxPoint - minPoint, false)
        this.tickSpacing = niceNum(range / (maxTicks - 1), true)
        this.niceMin = Math.floor(minPoint / tickSpacing) * tickSpacing
        this.niceMax = Math.ceil(maxPoint / tickSpacing) * tickSpacing
        println("${niceMin} ~ ${niceMax}, interval: ${tickSpacing}")
    }

    /**
     * Returns a "nice" number approximately equal to range Rounds
     * the number if round = true Takes the ceiling if round = false.

     * @param range the data range
     * *
     * @param round whether to round the result
     * *
     * @return a "nice" number to be used for the data range
     */
    private fun niceNum(range: Double, round: Boolean): Double {
        val exponent: Double
        /** exponent of range  */
        val fraction: Double
        /** fractional part of range  */
        val niceFraction: Double
        /** nice, rounded fraction  */

        exponent = Math.floor(Math.log10(range))
        fraction = range / Math.pow(10.0, exponent)

        if (round) {
            if (fraction < 1.5)
                niceFraction = 1.0
            else if (fraction < 3)
                niceFraction = 2.0
            else if (fraction < 7)
                niceFraction = 5.0
            else
                niceFraction = 10.0
        } else {
            if (fraction <= 1)
                niceFraction = 1.0
            else if (fraction <= 2)
                niceFraction = 2.0
            else if (fraction <= 5)
                niceFraction = 5.0
            else
                niceFraction = 10.0
        }
        return niceFraction * Math.pow(10.0, exponent)
    }

    /**
     * Sets the minimum and maximum data points for the axis.

     * @param minPoint the minimum data point on the axis
     * *
     * @param maxPoint the maximum data point on the axis
     */
    fun setMinMaxPoints(minPoint: Double, maxPoint: Double) {
        this.minPoint = minPoint
        this.maxPoint = maxPoint
        calculate()
    }

    /**
     * Sets maximum number of tick marks we're comfortable with

     * @param maxTicks the maximum number of tick marks for the axis
     */
    fun setMaxTicks(maxTicks: Double) {
        this.maxTicks = maxTicks
        calculate()
    }
}