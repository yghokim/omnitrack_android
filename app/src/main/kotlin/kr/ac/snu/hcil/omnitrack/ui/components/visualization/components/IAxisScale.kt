package kr.ac.snu.hcil.omnitrack.ui.components.visualization.components

/**
 * Created by Young-Ho on 9/8/2016.
 */
interface IAxisScale<T> {
    interface ITickFormat<T>{
        fun format(value: T, index: Int): String
    }

    fun setRealCoordRange(from: Float, to: Float): IAxisScale<T>
    val numTicks : Int
    fun getTickCoordAt(index: Int): Float
    fun getTickLabelAt(index: Int): String

    var tickFormat : ITickFormat<T>?

    fun getTickInterval(): Float

    operator fun get(domain: T): Float
}