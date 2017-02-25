package kr.ac.snu.hcil.omnitrack.core.visualization

import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.IChartInterface
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.utils.events.Event

/**
 * Created by younghokim on 16. 9. 7..
 */
abstract class ChartModel<T>(): IChartInterface<T> {

    abstract val name: String

    abstract val numDataPoints: Int
    abstract val isLoaded: Boolean

    val onReloaded = Event<Boolean>()

    private val queryRange = TimeSpan()
    protected var currentGranularity: Granularity = Granularity.WEEK
        private set

    var isLoading: Boolean = false
        private set

    private var invalidated: Boolean = true

    fun reload()
    {
        if (!isLoading) {
            isLoading = true
            invalidated = false
            onReload {
                success ->
                isLoading = false
                if (invalidated) {
                    reload()
                } else onReloaded.invoke(this, success)
            }
        } else {
            invalidate()
        }
    }

    abstract fun onReload(finished: (Boolean) -> Unit)
    abstract fun recycle()

    val isEmpty: Boolean get() = numDataPoints == 0

    abstract fun getChartDrawer(): AChartDrawer

    fun invalidate() {
        invalidated = true
    }

    fun setTimeScope(time: Long, scope: Granularity) {
        scope.convertToRange(time, queryRange)
        currentGranularity = scope
        invalidate()
    }


    fun getCurrentScopeGranularity(): Granularity{
        return currentGranularity
    }

    fun getTimeScope(): TimeSpan {
        return queryRange
    }


}