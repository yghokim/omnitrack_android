package kr.ac.snu.hcil.omnitrack.core.visualization.interfaces

/**
 * Created by Young-Ho on 9/9/2016.
 */

interface ITimeBinnedHeatMap{
    /**
     * float is normalized
     */
    data class CounterVector(val time: Long, val distribution: FloatArray )
}
