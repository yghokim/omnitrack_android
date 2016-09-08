package kr.ac.snu.hcil.omnitrack.core.visualization

import kr.ac.snu.hcil.omnitrack.core.OTTracker

/**
 * Created by younghokim on 16. 9. 7..
 */
abstract class TrackerChartModel<T>(val tracker: OTTracker) : ChartModel<T>() {
    override val name: String
        get() = tracker.name
}