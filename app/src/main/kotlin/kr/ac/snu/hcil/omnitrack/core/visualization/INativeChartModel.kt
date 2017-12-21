package kr.ac.snu.hcil.omnitrack.core.visualization

import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer

/**
 * Created by younghokim on 2017. 11. 23..
 */
interface INativeChartModel {
    fun getChartDrawer(): AChartDrawer
}