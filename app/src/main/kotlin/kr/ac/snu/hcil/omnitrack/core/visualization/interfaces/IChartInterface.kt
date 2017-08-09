package kr.ac.snu.hcil.omnitrack.core.visualization.interfaces

/**
 * Created by younghokim on 16. 9. 7..
 */
interface IChartInterface<T> {
    fun getDataPointAt(position: Int): T
    fun getDataPoints(): List<T>
}