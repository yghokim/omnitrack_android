package kr.ac.snu.hcil.omnitrack.core.visualization

import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO

/**
 * Created by Young-Ho Kim on 2016-09-08.
 */
abstract class CompoundAttributeChartModel<T>(open val attributes: List<OTAttributeDAO>, val parent: OTTrackerDAO, realm: Realm) : ChartModel<T>(realm)