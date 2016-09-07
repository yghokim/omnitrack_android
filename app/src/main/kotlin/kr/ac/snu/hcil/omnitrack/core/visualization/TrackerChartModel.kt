package kr.ac.snu.hcil.omnitrack.core.visualization

import kr.ac.snu.hcil.omnitrack.core.OTTracker

/**
 * Created by younghokim on 16. 9. 7..
 */
abstract class TrackerChartModel<T>(type: ChartType, val tracker: OTTracker) : ChartModel<T>(type) {
    override val name: String
        get() = tracker.name
}