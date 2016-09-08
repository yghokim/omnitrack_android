package kr.ac.snu.hcil.omnitrack.core.visualization.models

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.OTNumberAttribute
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartType
import kr.ac.snu.hcil.omnitrack.core.visualization.CompoundAttributeChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.ITimelineChart
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.ILineChartOnTime
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers.MultiLineChartDrawer
import java.math.BigDecimal
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-08.
 */
class TimelineComparisonLineChartModel(override val attributes: List<OTNumberAttribute>, parent: OTTracker)
: CompoundAttributeChartModel<ILineChartOnTime.LineData>(ChartType.TIMESPAN_TIMELINE, attributes, parent), ILineChartOnTime {

    override val isScopeControlSupported: Boolean = true

    override val name: String = OTApplication.app.resources.getString(R.string.msg_vis_numeric_line_timeline_title)

    override val numDataPoints: Int get() = data.size

    override val isLoaded: Boolean
        get() = _isLoaded

    private var _isLoaded = false

    private val data = ArrayList<ILineChartOnTime.LineData>()

    private val queryRange = TimeSpan()

    private val itemsCache = ArrayList<OTItem>()

    private val pointsCache = ArrayList<Pair<Long, BigDecimal>>()

    override fun reload() {
        data.clear()

        OTApplication.app.dbHelper.getItems(parent, queryRange, itemsCache)

        for (attribute in attributes) {
            for (item in itemsCache) {
                if (item.hasValueOf(attribute)) {
                    pointsCache.add(Pair(item.timestamp, item.getCastedValueOf<BigDecimal>(attribute)!!))
                }
            }

            data.add(
                    ILineChartOnTime.LineData(pointsCache.toTypedArray(), attribute)
            )

            pointsCache.clear()
        }

        println(data)

        itemsCache.clear()
        _isLoaded = true
    }

    override fun recycle() {
        data.clear()
    }

    override fun getChartDrawer(): AChartDrawer {
        val drawer = MultiLineChartDrawer()



        drawer.verticalAxis.labelPaint.isFakeBoldText = true
        drawer.verticalAxis.labelPaint.textSize = OTApplication.app.resources.getDimension(R.dimen.vis_axis_label_numeric_size)
        return drawer
    }

    override fun getDataPointAt(position: Int): ILineChartOnTime.LineData {
        return data[position]
    }

    override fun getDataPoints(): List<ILineChartOnTime.LineData> {
        return data
    }

    override fun setTimeScope(time: Long, scope: ITimelineChart.Granularity) {
        scope.convertToRange(time, queryRange)
    }

    override fun getTimeScope(): TimeSpan {
        return queryRange
    }


}