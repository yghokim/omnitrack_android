package kr.ac.snu.hcil.omnitrack.core

import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.ISingleNumberAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.models.DailyCountChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.models.LoggingHeatMapModel
import kr.ac.snu.hcil.omnitrack.core.visualization.models.TimeSeriesPlotModel
import kr.ac.snu.hcil.omnitrack.core.visualization.models.TimelineComparisonLineChartModel
import java.util.*

/**
 * Created by younghokim on 2017. 10. 16..
 */
object TrackerHelper {

    fun makeRecommendedChartModels(trackerDao: OTTrackerDAO, realm: Realm): Array<ChartModel<*>> {

        val list = ArrayList<ChartModel<*>>()

        val timePointAttributes = trackerDao.attributes.filter { it.isHidden == false && it.isInTrashcan == false && it.type == OTAttributeManager.TYPE_TIME }


        //generate tracker-level charts

        list += DailyCountChartModel(trackerDao, realm, timePointAttributes.firstOrNull())
        list += LoggingHeatMapModel(trackerDao, realm, timePointAttributes.firstOrNull())

        //add line timeline if numeric variables exist
        val numberAttrs = trackerDao.makeAttributesQuery(false,false).equalTo("type", OTAttributeManager.TYPE_NUMBER).findAll()
        if (numberAttrs.isNotEmpty()) {
            list.add(TimelineComparisonLineChartModel(numberAttrs, trackerDao, realm))
        }

        //add time-value scatterplots
        val singleNumberAttributes = trackerDao.attributes.filter { it.isHidden == false && it.isInTrashcan == false && it.getHelper() is ISingleNumberAttributeHelper }
        for (timeAttr in timePointAttributes) {
            for (numAttr in singleNumberAttributes) {
                list.add(TimeSeriesPlotModel(trackerDao, timeAttr, numAttr, realm))
            }
        }

        for (attribute in trackerDao.attributes.filter { it.isHidden == false && it.isInTrashcan == false }) {
            list.addAll(attribute.getHelper().makeRecommendedChartModels(attribute, realm))
        }

        return list.toTypedArray()
    }
}