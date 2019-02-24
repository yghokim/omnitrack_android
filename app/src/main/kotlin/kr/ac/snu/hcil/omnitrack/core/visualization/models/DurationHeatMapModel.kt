package kr.ac.snu.hcil.omnitrack.core.visualization.models

import android.content.Context
import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.ISingleNumberAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTRatingAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.visualization.IWebBasedChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.TrackerChartModel
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.QuantizedTimeScale
import kr.ac.snu.hcil.omnitrack.utils.RatingOptions

/**
 * Created by younghokim on 2017. 11. 25..
 */
open class DurationHeatMapModel(tracker: OTTrackerDAO, timeSpanAttribute: OTAttributeDAO, val numericAttribute: OTAttributeDAO, realm: Realm, val context: Context) : TrackerChartModel<DurationHeatMapModel.DataPoint>(tracker, realm), IWebBasedChartModel {

    data class DataPoint(val dateIndex: Int, val fromDateRatio: Float, val toDateRatio: Float, val value: Double, val cutOnLeft: Boolean, val cutOnRight: Boolean) {
        fun toJsonString(): String {
            return "{\"i\": $dateIndex, \"fromRatio\":$fromDateRatio, \"toRatio\":$toDateRatio, \"value\": $value, \"cutL\":$cutOnLeft, \"cutR\":$cutOnRight}"
        }
    }

    val timeSpanAttributeLocalId: String = timeSpanAttribute.localId
    val numericAttributeLocalId: String = numericAttribute.localId

    protected var cachedDates: List<Long>? = null
    protected var intrinsicValueMin: Double? = null
    protected var intrinsicValueMax: Double? = null
    protected var intrinsicValueLevels: Int? = null

    init {
        resolveIntrinsicRanges(numericAttribute)

        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    protected fun resolveIntrinsicRanges(attr: OTAttributeDAO) {
        if (attr.type == OTAttributeManager.TYPE_RATING) {
            val ratingOptions = (attr.getHelper(context) as OTRatingAttributeHelper).getRatingOptions(attr)
            this.intrinsicValueLevels = when (ratingOptions.type) {
                RatingOptions.DisplayType.Likert -> (ratingOptions.rightMost - ratingOptions.leftMost)
                RatingOptions.DisplayType.Star -> ratingOptions.stars
            }

            when (ratingOptions.type) {
                RatingOptions.DisplayType.Likert -> {
                    intrinsicValueMax = ratingOptions.rightMost.toDouble()
                    intrinsicValueMin = ratingOptions.leftMost.toDouble()
                }
                RatingOptions.DisplayType.Star -> {
                    intrinsicValueMin = if (ratingOptions.isFractional) 0.5 else 1.0
                    intrinsicValueMax = ratingOptions.stars.toDouble()
                }
            }
        }
    }

    override fun reloadData(): Single<List<DataPoint>> {
        return Single.defer {
            val items = dbManager.getItemsQueriedWithTimeAttribute(tracker.objectId, getTimeScope(), timeSpanAttributeLocalId, realm)
                    .filter {
                        it.getValueOf(numericAttributeLocalId) != null
                    }

            val data = ArrayList<DataPoint>()

            val xScale = QuantizedTimeScale(context)
            xScale.setDomain(getTimeScope().from, getTimeScope().to)
            xScale.quantize(currentGranularity)
            cachedDates = xScale.binPointsOnDomain.toList()

            for (item in items) {
                val timeSpan = item.getValueOf(timeSpanAttributeLocalId) as TimeSpan

                if (timeSpan.from >= xScale.domainTimeMax || timeSpan.to <= xScale.domainTimeMin) {
                    continue
                }

                val helper = numericAttribute.getHelper(context)
                val value: Double = if (helper is ISingleNumberAttributeHelper) {
                    helper.convertValueToSingleNumber(item.getValueOf(numericAttributeLocalId)!!, numericAttribute)
                } else {
                    continue
                }

                //quantize
                quantizeLoop@ for (i in 0 until xScale.binPointsOnDomain.size) {
                    val binStart = xScale.binPointsOnDomain[i]
                    val binEnd = binStart + xScale.binPointsOnDomain[1] - xScale.binPointsOnDomain[0]

                    if (binEnd <= timeSpan.from) continue@quantizeLoop

                    val startingPoint = Math.max(timeSpan.from, binStart)

                    val endPoint = Math.min(timeSpan.to, binEnd)

                    data.add(DataPoint(i,
                            (startingPoint - binStart).toFloat() / (binEnd - binStart),
                            (endPoint - binStart).toFloat() / (binEnd - binStart),
                            value,
                            (timeSpan.from < binStart),
                            (timeSpan.to > binEnd)
                    ))

                    if (timeSpan.to <= binEnd) {
                        break@quantizeLoop
                    }
                }
            }

            return@defer Single.just(data)
        }
    }

    override fun getDataInJsonString(): String {
        val string = "{\"dates\":[${cachedDates?.joinToString(", ")}], \"intrinsicValueRange\":{\"from\":$intrinsicValueMin, \"to\":$intrinsicValueMax, \"level\": $intrinsicValueLevels}, \"data\":[${cachedData.joinToString(", ") { it.toJsonString() }}]}"
        println(string)
        return string
    }

    override fun getChartTypeCommand(): String {
        return "duration-value-plot"
    }


}