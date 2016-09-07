package kr.ac.snu.hcil.omnitrack.core.visualization.models

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.visualization.AttributeChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartType
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.ICategoricalBarChart
import java.util.*

/**
 * Created by younghokim on 16. 9. 7..
 */
class ChoiceCategoricalBarChartModel : AttributeChartModel, ICategoricalBarChart {

    private var loaded = false
    private val data = ArrayList<ICategoricalBarChart.Point>()

    private val itemsCache = ArrayList<OTItem>()

    override val isLoaded: Boolean get() = !loaded

    override val numDataPoints: Int get() = data.size

    override fun recycle() {
        data.clear()
    }

    override fun reload() {
        val tracker = attribute.owner
        if (tracker != null) {
            itemsCache.clear()
            OTApplication.app.dbHelper.getItems(tracker, itemsCache)
        }
    }

    override fun getDataPointAt(position: Int): ICategoricalBarChart.Point {
        return data[position]
    }

    constructor(attribute: OTAttribute<out Any>) : super(ChartType.BARCHART_CATEGORICAL, attribute)

}