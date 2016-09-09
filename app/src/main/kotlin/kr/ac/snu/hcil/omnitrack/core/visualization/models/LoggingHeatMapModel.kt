package kr.ac.snu.hcil.omnitrack.core.visualization.models

import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.Granularity
import kr.ac.snu.hcil.omnitrack.core.visualization.TrackerChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.ICategoricalBarChart
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.ITimeBinnedHeatMap
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.QuantizedTimeScale
import java.util.*

/**
 * Created by Young-Ho on 9/9/2016.
 */

/*
class LoggingHeatMapModel(tracker: OTTracker): TrackerChartModel<ITimeBinnedHeatMap.Cell>(tracker), ITimeBinnedHeatMap {

    private var _isLoaded = false
    private val data = ArrayList<ITimeBinnedHeatMap.Cell>()

    override fun getDataPoints(): List<ITimeBinnedHeatMap.Cell> {
        return data
    }

    override val numDataPoints: Int get() = data.size

    override val isLoaded: Boolean get()= _isLoaded

    override fun onReload() {

        val scale = QuantizedTimeScale()
        scale.setDomain(getTimeScope().from, getTimeScope().to)
        scale.quantize(currentGranularity)




        _isLoaded = true
    }

    override fun recycle() {
        data.clear()
    }

    override fun getChartDrawer(): AChartDrawer {

    }

    override fun getDataPointAt(position: Int): ITimeBinnedHeatMap.Cell {
        return data[position]
    }

}*/