package kr.ac.snu.hcil.omnitrack.core

import android.content.Context
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.fields.OTFieldManager
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.ISingleNumberFieldHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.models.DailyCountChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.models.DurationHeatMapModel
import kr.ac.snu.hcil.omnitrack.core.visualization.models.LoggingHeatMapModel
import kr.ac.snu.hcil.omnitrack.core.visualization.models.TimeSeriesPlotModel
import java.util.*

/**
 * Created by younghokim on 2017. 10. 16..
 */
object TrackerHelper {

    fun makeRecommendedChartModels(trackerDao: OTTrackerDAO, realm: Realm, context: Context): Array<ChartModel<*>> {

        val list = ArrayList<ChartModel<*>>()

        val timePointAttributes = trackerDao.fields.filter { !it.isHidden && !it.isInTrashcan && it.type == OTFieldManager.TYPE_TIME }

        val timeSpanAttributes = trackerDao.fields.filter { !it.isHidden && !it.isInTrashcan && it.type == OTFieldManager.TYPE_TIMESPAN }

        val singleNumberAttributes = trackerDao.fields.filter { !it.isHidden && !it.isInTrashcan && it.getHelper(context) is ISingleNumberFieldHelper }

        //generate tracker-level charts

        list += DailyCountChartModel(trackerDao, realm, context, timePointAttributes.firstOrNull())
        list += LoggingHeatMapModel(trackerDao, realm, context, timePointAttributes.firstOrNull())

        //durationTimeline
        for (timeSpanAttr in timeSpanAttributes) {
            for (numAttr in singleNumberAttributes) {
                list.add(DurationHeatMapModel(trackerDao, timeSpanAttr, numAttr, realm, context))
            }
        }

        //add line timeline if numeric variables exist
        /*
        val numberAttrs = trackerDao.makeAttributesQuery(false,false).equalTo("type", OTFieldManager.TYPE_NUMBER).findAll()
        if (numberAttrs.isNotEmpty()) {
            list.add(TimelineComparisonLineChartModel(numberAttrs, trackerDao, realm))
        }*/

        //add time-value scatterplots
        for (timeAttr in timePointAttributes) {
            for (numAttr in singleNumberAttributes) {
                list.add(TimeSeriesPlotModel(trackerDao, timeAttr, numAttr, realm, context))
            }
        }

        for (field in trackerDao.fields.filter { !it.isHidden && !it.isInTrashcan }) {
            list.addAll(field.getHelper(context).makeRecommendedChartModels(field, realm))
        }

        return list.toTypedArray()
    }
}