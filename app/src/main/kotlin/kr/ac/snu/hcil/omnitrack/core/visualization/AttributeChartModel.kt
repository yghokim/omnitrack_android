package kr.ac.snu.hcil.omnitrack.core.visualization

import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO

/**
 * Created by younghokim on 16. 9. 7..
 */
abstract class AttributeChartModel<T>(open val field: OTFieldDAO, realm: Realm) : ChartModel<T>(realm) {
    override val name: String
        get() = this.field.name
}