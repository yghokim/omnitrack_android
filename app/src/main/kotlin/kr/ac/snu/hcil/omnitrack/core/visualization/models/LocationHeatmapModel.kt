package kr.ac.snu.hcil.omnitrack.core.visualization.models

import com.google.android.gms.maps.model.LatLng
import io.reactivex.Single
import io.realm.Realm
import io.realm.Sort
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel

/**
 * Created by younghokim on 2017. 12. 4..
 */
class LocationHeatmapModel(val trackerId: String, locationAttribute: OTAttributeDAO, timeAttribute: OTAttributeDAO? = null, realm: Realm) : ChartModel<Pair<Double, Double>>(realm) {

    private val locationAttributeLocalId: String = locationAttribute.localId
    private val locationAttributeName: String = locationAttribute.name

    private val timeAttributeLocalId: String? = timeAttribute?.localId
    private val timeAttributeName: String? = timeAttribute?.name

    override val name: String
        get() = "Pinned Locations"

    override fun reloadData(): Single<List<Pair<Double, Double>>> {
        return if (timeAttributeLocalId != null) {

            Single.just(dbManager.getItemsQueriedWithTimeAttribute(trackerId, getTimeScope(),
                    timeAttributeLocalId, realm).sortedBy { (it.getValueOf(timeAttributeLocalId) as TimePoint).timestamp })
        } else {
            dbManager
                    .makeItemsQuery(trackerId, getTimeScope(), realm)
                    .sort("timestamp", Sort.ASCENDING)
                    .findAllAsync()
                    .asFlowable()
                    .filter { it.isLoaded }
                    .firstOrError()
        }.map { items ->
            items.mapNotNull { item ->
                (item.getValueOf(locationAttributeLocalId) as? LatLng)?.let { Pair(it.latitude, it.longitude) }
            }
        }.doOnSuccess { values ->
            cachedData.addAll(values)
        }
    }

}