package kr.ac.snu.hcil.omnitrack.core.visualization

import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTAttributeDAO

/**
 * Created by younghokim on 16. 9. 7..
 */
abstract class AttributeChartModel<T>(open val attribute: OTAttributeDAO, realm: Realm) : ChartModel<T>(realm) {
    override val name: String
        get() = attribute.name
}