package kr.ac.snu.hcil.omnitrack.core.visualization

import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.IChartInterface
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.utils.events.Event

/**
 * Created by younghokim on 16. 9. 7..
 */
abstract class ChartModel<T>(val type: ChartType): IChartInterface<T> {

    abstract val name: String

    abstract val numDataPoints: Int
    abstract val isLoaded: Boolean

    val onReloaded = Event<Boolean>()

    abstract fun reload()
    abstract fun recycle()

    val isEmpty: Boolean get() = numDataPoints == 0

    abstract fun getChartDrawer(): AChartDrawer
}