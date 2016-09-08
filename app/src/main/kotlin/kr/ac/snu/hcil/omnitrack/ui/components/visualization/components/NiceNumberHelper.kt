package kr.ac.snu.hcil.omnitrack.ui.components.visualization.components

/**
 * Created by Young-Ho Kim on 2016-09-08.
 */
open class NiceNumberHelper {

    var niceMin: Float = 0f
    var niceMax: Float = 0f
    var niceTickSpacing: Float = 0f


    fun calculate(dataMin: Float, dataMax: Float, isInteger: Boolean) {

        niceTickSpacing = getIdealBinSizeOfRange(dataMin, dataMax)
        if (isInteger) {
            if (niceTickSpacing < 1) {
                niceTickSpacing = 1f
            }
        }

        if (dataMin < 0 && dataMin > 0) {

            niceMin = calcPivotedDimension(dataMin, 0f, niceTickSpacing, false)

            niceMax = calcPivotedDimension(0f, dataMax, niceTickSpacing, true)
        } else {
            var originalMin = dataMin
            var originalMax = dataMax

            if (dataMax - dataMin == 0f) {
                originalMin -= niceTickSpacing
                originalMax += niceTickSpacing
            }

            niceMin = Math.floor(originalMin / niceTickSpacing.toDouble()).toFloat() * niceTickSpacing
            niceMax = Math.ceil(originalMax / niceTickSpacing.toDouble()).toFloat() * niceTickSpacing
        }
    }


    private fun calcPivotedDimension(from: Float, to: Float, binSize: Float, pivotLeft: Boolean): Float {
        var currentBorder = 0f
        var numBins = 0

        if (pivotLeft) {
            currentBorder = from
            while (currentBorder <= to) {
                currentBorder += binSize;
                numBins++;
            }
        } else {
            currentBorder = to
            while (currentBorder >= from) {
                currentBorder -= binSize;
                numBins++;
            }
        }

        return currentBorder
    }

    protected fun getIdealBinSizeOfRange(from: Float, to: Float): Float {
        if (to - from == 0f) return 1f

        var rangeSize = to - from

        //comment by shlyi
        //this code is for covering the situation when the rangeSize is really small (e.g. 0.000001)
        var multied = 0
        if (rangeSize < 1) {
            while (rangeSize < 1) {
                rangeSize *= 10.0f
                multied++
            }
        }

        val nOfTen = Math.log10(rangeSize.toDouble()).toInt()
        val multiplier = Math.pow(10.0, nOfTen.toDouble()).toInt()

        val firstDigit = rangeSize / multiplier

        var binSize: Float

        if (firstDigit <= 1)
            binSize = 0.1f * multiplier
        else if (firstDigit <= 3)
            binSize = 0.25f * multiplier
        else if (firstDigit <= 5)
            binSize = 0.5f * multiplier
        else
            binSize = multiplier.toFloat()

        while (multied-- != 0) {
            binSize /= 10f
        }

        //System.Diagnostics.Debug.WriteLine("Range : " + range + ", Recommended Bin Size : " + binSize);

        return binSize
    }


}
