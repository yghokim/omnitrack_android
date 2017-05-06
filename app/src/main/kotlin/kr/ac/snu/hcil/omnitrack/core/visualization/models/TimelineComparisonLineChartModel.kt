package kr.ac.snu.hcil.omnitrack.core.visualization.models

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.OTNumberAttribute
import kr.ac.snu.hcil.omnitrack.core.database.DatabaseManager
import kr.ac.snu.hcil.omnitrack.core.visualization.CompoundAttributeChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.ILineChartOnTime
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.QuantizedTimeScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers.MultiLineChartDrawer
import org.apache.commons.math3.stat.StatUtils
import rx.internal.util.SubscriptionList
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

    private val values = ArrayList<BigDecimal>()

    private val subscriptions = SubscriptionList()

    override fun onReload(finished: (Boolean) -> Unit) {
        data.clear()
        subscriptions.clear()

        val xScale = QuantizedTimeScale()
        xScale.setDomain(getTimeScope().from, getTimeScope().to)
        xScale.quantize(currentGranularity)

        subscriptions.add(
                DatabaseManager.loadItems(parent, getTimeScope(), DatabaseManager.Order.ASC).subscribe {
                    items ->

                    var currentItemPointer = 0

                    val itemBinCache = ArrayList<OTItem>()

                    val attrPivotedPoints = HashMap<OTAttribute<out Any>, MutableList<Pair<Long, BigDecimal>>>()
                    attributes.forEach {
                        attrPivotedPoints[it] = ArrayList()
                    }

                    for (xIndex in 0..xScale.numTicks - 1) {
                        val from = xScale.binPointsOnDomain[xIndex]
                        val to = if (xIndex < xScale.numTicks - 1) xScale.binPointsOnDomain[xIndex + 1]
                        else getTimeScope().to

                        itemBinCache.clear()

                        if (currentItemPointer < items.size) {
                            var timestamp = items[currentItemPointer].timestamp
                            while (timestamp < to) {
                                if (timestamp >= from) {
                                    itemBinCache.add(items[currentItemPointer])
                                }

                                currentItemPointer++
                                if (currentItemPointer >= items.size) {
                                    break
                                }
                                timestamp = items[currentItemPointer].timestamp
                        }
                        }


                        for (attribute in attributes) {
                            values.clear()

                            val numPoints = OTItem.extractNotNullValues(itemBinCache, attribute, values)
                            if (numPoints > 0) {
                                attrPivotedPoints[attribute]?.add(Pair(from, BigDecimal(StatUtils.mean(values.map { it.toDouble() }.toDoubleArray()))))
                        }
                    }

                }

                    synchronized(data) {
                    data.clear()
                    attrPivotedPoints.mapTo(data) {
                        ILineChartOnTime.TimeSeriesTrendData(it.value.toTypedArray(), it.key)
                    }
                }

                    values.clear()


                    _isLoaded = true
                    finished.invoke(true)
            }
        )
    }

    override fun recycle() {
        data.clear()
        subscriptions.clear()
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