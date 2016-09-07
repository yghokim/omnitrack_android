package kr.ac.snu.hcil.omnitrack.core.visualization

import kr.ac.snu.hcil.omnitrack.core.OTTracker

/**
 * Created by younghokim on 16. 9. 7..
 */
abstract class TrackerChartModel(type: ChartType, val tracker: OTTracker) : ChartModel(type) {
}