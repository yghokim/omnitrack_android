package kr.ac.snu.hcil.omnitrack.core.visualization.models

import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.ISingleNumberAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import kr.ac.snu.hcil.omnitrack.core.visualization.IWebBasedChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.TrackerChartModel

/**
 * Created by younghokim on 2017. 11. 24..
 */
class TimeSeriesPlotModel(tracker: OTTrackerDAO, protected val timeAttribute: OTAttributeDAO, protected val yAttribute: OTAttributeDAO, realm: Realm) : TrackerChartModel<Pair<Long, Double>>(tracker, realm), IWebBasedChartModel {

    private val timeAttributeLocalId = timeAttribute.localId
    private val yValueAttributeLocalId = yAttribute.localId

    protected val timeAttributeName: String = timeAttribute.name
    protected val yAttributeName: String = yAttribute.name

    override val name: String
        get() {
            return String.format(OTApp.getString(R.string.msg_vis_format_time_number_plot_title), yAttributeName, timeAttributeName)
        }

    init {
        OTApp.instance.applicationComponent.inject(this)
    }

    override fun reloadData(): Single<List<Pair<Long, Double>>> {
        return Single.defer {
            val yAttributeHelper = yAttribute.let { OTAttributeManager.getAttributeHelper(it.type) }
            if (yAttributeHelper is ISingleNumberAttributeHelper) {
                val items = dbManager.getItemsQueriedWithTimeAttribute(tracker.objectId, getTimeScope(), timeAttributeLocalId, realm)
                return@defer Single.just(
                        items.filter { it.getValueOf(yValueAttributeLocalId) != null }.map {
                            Pair(
                                    (it.getValueOf(timeAttributeLocalId) as TimePoint).timestamp,
                                    yAttributeHelper.convertValueToSingleNumber(it.getValueOf(yValueAttributeLocalId)!!, yAttribute))
                        }
                )
            } else return@defer Single.just<List<Pair<Long, Double>>>(emptyList())
        }
    }

    override fun getDataInJsonString(): String {
        val timeScope = getTimeScope()
        val json = "{\"range\":{\"from\":${timeScope.from}, \"to\":${timeScope.to}, \"timezone\":\"${timeScope.timeZone.id}\"}, \"labels\":{\"t\":\"$timeAttributeName\", \"y\":\"${yAttributeName}\"}, \"data\":[${cachedData.map { "{\"t\":${it.first}, \"y\": ${it.second}}" }.joinToString(", ")}]}"
        println(json)
        return json
    }

    override fun getChartTypeCommand(): String {
        return "time-number-plot"
    }


}