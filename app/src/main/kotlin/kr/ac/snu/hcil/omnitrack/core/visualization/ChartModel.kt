package kr.ac.snu.hcil.omnitrack.core.visualization

import android.content.Context
import android.text.format.DateUtils
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.IChartInterface
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.utils.TimeHelper
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kr.ac.snu.hcil.omnitrack.utils.getYear
import java.util.*

/**
 * Created by younghokim on 16. 9. 7..
 */
abstract class ChartModel<T>(): IChartInterface<T> {

    abstract val name: String

    abstract val numDataPoints: Int
    abstract val isLoaded: Boolean

    val onReloaded = Event<Boolean>()

    protected val queryRange = TimeSpan()
    protected var currentGranularity: Granularity = Granularity.WEEK
        private set

    fun reload()
    {
        onReload()
        onReloaded.invoke(this, true)
    }

    abstract fun onReload()
    abstract fun recycle()

    val isEmpty: Boolean get() = numDataPoints == 0

    abstract fun getChartDrawer(): AChartDrawer

    fun setTimeScope(time: Long, scope: Granularity) {
        scope.convertToRange(time, queryRange)
        currentGranularity = scope
    }


    fun getCurrentScopeGranularity(): Granularity{
        return currentGranularity
    }

    fun getTimeScope(): TimeSpan {
        return queryRange
    }


}