package kr.ac.snu.hcil.omnitrack.core.visualization

import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute

/**
 * Created by Young-Ho Kim on 2016-09-08.
 */
abstract class CompoundAttributeChartModel<T>(open val attributes: List<OTAttribute<out Any>>, val parent: OTTracker) : ChartModel<T>() {
}