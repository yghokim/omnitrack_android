package kr.ac.snu.hcil.omnitrack.core.visualization.models

import android.content.Context
import io.reactivex.Single
import io.realm.Realm
import io.realm.Sort
import kr.ac.snu.hcil.android.common.isNumericPrimitive
import kr.ac.snu.hcil.android.common.toBigDecimal
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.visualization.CompoundAttributeChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.INativeChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.ILineChartOnTime
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.QuantizedTimeScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers.MultiLineChartDrawer
import java.math.BigDecimal
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-08.
 */
class TimelineComparisonLineChartModel(attributes: List<OTAttributeDAO>, parent: OTTrackerDAO, realm: Realm, val context: Context)
    : CompoundAttributeChartModel<ILineChartOnTime.TimeSeriesTrendData>(attributes, parent, realm), ILineChartOnTime, INativeChartModel {

    override val name: String = context.resources.getString(R.string.msg_vis_numeric_line_timeline_title)

    init{
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    override fun reloadData(): Single<List<ILineChartOnTime.TimeSeriesTrendData>> {
        val data = ArrayList<ILineChartOnTime.TimeSeriesTrendData>()
        val values = ArrayList<BigDecimal>()

        val xScale = QuantizedTimeScale(context)
        xScale.setDomain(getTimeScope().from, getTimeScope().to)
        xScale.quantize(currentGranularity)

        return dbManager
                .makeItemsQuery(parent.objectId, getTimeScope(), realm)
                .sort("timestamp", Sort.ASCENDING)
                .findAllAsync()
                .asFlowable()
                .filter { it.isLoaded && it.isValid }.firstOrError().map { items ->

            var currentItemPointer = 0

            val itemBinCache = ArrayList<OTItemDAO>()

            val attrPivotedPoints = HashMap<String, MutableList<Pair<Long, BigDecimal>>>()
            attributes.forEach {
                attrPivotedPoints[it.localId] = ArrayList()
            }

                    for (xIndex in 0 until xScale.numTicks) {
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
                        attrPivotedPoints[attribute.localId]?.add(Pair(from, BigDecimal(values.asSequence().map { it.toFloat() }.average())))
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
        val drawer = MultiLineChartDrawer(context)



        drawer.verticalAxis.labelPaint.isFakeBoldText = true
        drawer.verticalAxis.labelPaint.textSize = context.resources.getDimension(R.dimen.vis_axis_label_numeric_size)
        return drawer
    }
}