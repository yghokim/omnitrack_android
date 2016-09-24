package kr.ac.snu.hcil.omnitrack.core.visualization.models

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.OTNumberAttribute
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.visualization.CompoundAttributeChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.ILineChartOnTime
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.QuantizedTimeScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers.MultiLineChartDrawer
import org.apache.commons.math3.stat.StatUtils
import java.math.BigDecimal
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-08.
 */
class TimelineComparisonLineChartModel(override val attributes: List<OTNumberAttribute>, parent: OTTracker)
: CompoundAttributeChartModel<ILineChartOnTime.TimeSeriesTrendData>(attributes, parent), ILineChartOnTime {

    override val name: String = OTApplication.app.resources.getString(R.string.msg_vis_numeric_line_timeline_title)

    override val numDataPoints: Int get() = data.size

    override val isLoaded: Boolean
        get() = _isLoaded

    private var _isLoaded = false

    private val data = ArrayList<ILineChartOnTime.TimeSeriesTrendData>()


    private val itemsCache = ArrayList<OTItem>()

    private val pointsCache = ArrayList<Pair<Long, BigDecimal>>()

    private val values = ArrayList<BigDecimal>()


    override fun onReload() {
        data.clear()


        val xScale = QuantizedTimeScale()
        xScale.setDomain(getTimeScope().from, getTimeScope().to)
        xScale.quantize(currentGranularity)

        for (attribute in attributes) {

            pointsCache.clear()

            for (xIndex in 0..xScale.numTicks - 1) {
                itemsCache.clear()
                val from = xScale.binPointsOnDomain[xIndex]
                val to = if (xIndex < xScale.numTicks - 1) xScale.binPointsOnDomain[xIndex + 1]
                else getTimeScope().to

                OTApplication.app.dbHelper.getItems(parent, TimeSpan.fromPoints(from, to), itemsCache, true)

                values.clear()

                val numPoints = OTItem.extractNotNullValues(itemsCache, attribute, values)
                if (numPoints > 0) {
                    pointsCache.add(
                            Pair(from, BigDecimal(StatUtils.mean(values.map { it.toDouble() }.toDoubleArray())))
                    )
                }
            }

            data.add(
                    ILineChartOnTime.TimeSeriesTrendData(pointsCache.toTypedArray(), attribute)
            )
        }

        println(data)

        values.clear()
        pointsCache.clear()
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

    override fun getDataPointAt(position: Int): ILineChartOnTime.TimeSeriesTrendData {
        return data[position]
    }

    override fun getDataPoints(): List<ILineChartOnTime.TimeSeriesTrendData> {
        return data
    }
}