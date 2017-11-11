package kr.ac.snu.hcil.omnitrack.core.visualization.models

import io.reactivex.Single
import io.realm.Realm
import io.realm.Sort
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.visualization.CompoundAttributeChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.ILineChartOnTime
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.QuantizedTimeScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers.MultiLineChartDrawer
import kr.ac.snu.hcil.omnitrack.utils.isNumericPrimitive
import kr.ac.snu.hcil.omnitrack.utils.toBigDecimal
import java.math.BigDecimal
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-08.
 */
class TimelineComparisonLineChartModel(attributes: List<OTAttributeDAO>, parent: OTTrackerDAO, realm: Realm)
    : CompoundAttributeChartModel<ILineChartOnTime.TimeSeriesTrendData>(attributes, parent, realm), ILineChartOnTime {

    override val name: String = OTApp.instance.resourcesWrapped.getString(R.string.msg_vis_numeric_line_timeline_title)

    init{
        OTApp.instance.applicationComponent.inject(this)
    }

    override fun reloadData(): Single<List<ILineChartOnTime.TimeSeriesTrendData>> {
        val data = ArrayList<ILineChartOnTime.TimeSeriesTrendData>()
        val values = ArrayList<BigDecimal>()

        val xScale = QuantizedTimeScale()
        xScale.setDomain(getTimeScope().from, getTimeScope().to)
        xScale.quantize(currentGranularity)

        return dbManager
                .makeItemsQuery(parent.objectId, getTimeScope(), realm)
                .findAllSortedAsync("timestamp", Sort.ASCENDING)
                .asFlowable()
                .filter { it.isLoaded == true && it.isValid }.firstOrError().map { items ->

            var currentItemPointer = 0

            val itemBinCache = ArrayList<OTItemDAO>()

            val attrPivotedPoints = HashMap<String, MutableList<Pair<Long, BigDecimal>>>()
            attributes.forEach {
                attrPivotedPoints[it.localId] = ArrayList()
            }

            for (xIndex in 0..xScale.numTicks - 1) {
                val from = xScale.binPointsOnDomain[xIndex]
                val to = if (xIndex < xScale.numTicks - 1) xScale.binPointsOnDomain[xIndex + 1]
                else getTimeScope().to

                itemBinCache.clear()

                if (currentItemPointer < items.size) {
                    var timestamp = items[currentItemPointer]!!.timestamp
                    while (timestamp < to) {
                        if (timestamp >= from) {
                            itemBinCache.add(items[currentItemPointer]!!)
                        }

                        currentItemPointer++
                        if (currentItemPointer >= items.size) {
                            break
                        }
                        timestamp = items[currentItemPointer]!!.timestamp
                    }
                }


                for (attribute in attributes) {
                    values.clear()

                    var count = 0

                    for (item in itemBinCache) {
                        val value = item.getValueOf(attribute.localId)
                        if (value != null) {
                            if (isNumericPrimitive(value)) {
                                values.add(toBigDecimal(value))
                                count++
                            }
                        }
                    }
                    if (count > 0) {
                        attrPivotedPoints[attribute.localId]?.add(Pair(from, BigDecimal(values.map { it.toFloat() }.average())))
                    }
                }
            }

            synchronized(data) {
                data.clear()
                attrPivotedPoints.mapTo(data) {
                    ILineChartOnTime.TimeSeriesTrendData(it.value.toTypedArray())
                }
            }
            data
        }
    }

    override fun getChartDrawer(): AChartDrawer {
        val drawer = MultiLineChartDrawer()



        drawer.verticalAxis.labelPaint.isFakeBoldText = true
        drawer.verticalAxis.labelPaint.textSize = OTApp.instance.resourcesWrapped.getDimension(R.dimen.vis_axis_label_numeric_size)
        return drawer
    }
}