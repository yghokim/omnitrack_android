package kr.ac.snu.hcil.omnitrack.core.visualization.models

import android.content.Context
import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.fields.OTFieldManager
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.ISingleNumberFieldHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.types.TimePoint
import kr.ac.snu.hcil.omnitrack.core.visualization.IWebBasedChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.TrackerChartModel
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 11. 24..
 */
class TimeSeriesPlotModel(tracker: OTTrackerDAO, protected val timeField: OTFieldDAO, protected val yField: OTFieldDAO, realm: Realm, val context: Context) : TrackerChartModel<Pair<Long, Double>>(tracker, realm), IWebBasedChartModel {

    private val timeAttributeLocalId = timeField.localId
    private val yValueAttributeLocalId = yField.localId

    protected val timeAttributeName: String = timeField.name
    protected val yAttributeName: String = yField.name

    override val name: String
        get() {
            return String.format(context.getString(R.string.msg_vis_format_time_number_plot_title), yAttributeName, timeAttributeName)
        }

    @Inject
    protected lateinit var fieldManager: OTFieldManager

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    override fun reloadData(): Single<List<Pair<Long, Double>>> {
        return Single.defer {
            val yAttributeHelper = yField.let { fieldManager.get(it.type) }
            if (yAttributeHelper is ISingleNumberFieldHelper) {
                val items = dbManager.getItemsQueriedWithTimeAttribute(tracker._id, getTimeScope(), timeAttributeLocalId, realm)
                return@defer Single.just(
                        items.asSequence().filter { it.getValueOf(yValueAttributeLocalId) != null }.map {
                            Pair(
                                    (it.getValueOf(timeAttributeLocalId) as TimePoint).timestamp,
                                    yAttributeHelper.convertValueToSingleNumber(it.getValueOf(yValueAttributeLocalId)!!, yField))
                        }.toList()
                )
            } else return@defer Single.just<List<Pair<Long, Double>>>(emptyList())
        }
    }

    override fun getDataInJsonString(): String {
        val timeScope = getTimeScope()
        val json = "{\"range\":{\"from\":${timeScope.from}, \"to\":${timeScope.to}, \"timezone\":\"${timeScope.timeZone.id}\"}, \"labels\":{\"t\":\"$timeAttributeName\", \"y\":\"$yAttributeName\"}, \"data\":[${cachedData.joinToString(", ") { "{\"t\":${it.first}, \"y\": ${it.second}}" }}]}"
        println(json)
        return json
    }

    override fun getChartTypeCommand(): String {
        return "time-number-plot"
    }


}