package kr.ac.snu.hcil.omnitrack.core.visualization

import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute

/**
 * Created by younghokim on 16. 9. 7..
 */
abstract class AttributeChartModel<T>(type: ChartType, open val attribute: OTAttribute<out Any>) : ChartModel<T>(type) {
    override val name: String
        get() = attribute.name
}