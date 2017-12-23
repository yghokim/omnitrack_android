package kr.ac.snu.hcil.omnitrack.core.visualization

import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTrackerDAO

/**
 * Created by younghokim on 16. 9. 7..
 */
abstract class TrackerChartModel<T>(val tracker: OTTrackerDAO, realm: Realm) : ChartModel<T>(realm) {
    override val name: String
        get() = tracker.name
}