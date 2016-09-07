package kr.ac.snu.hcil.omnitrack.ui.components.visualization.components

/**
 * Created by Young-Ho on 9/8/2016.
 */
interface IAxisScale {
    fun setRealCoordRange(from: Float, to: Float)
    val numTicks : Int
    fun getTickCoordAt(index: Int): Float
    fun getTickLabelAt(index: Int): String

    fun getTickInterval(): Float
}