package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.view.View
import it.sephiroth.android.library.tooltip.Tooltip
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-11-03.
 */
object TooltipHelper {
    fun makeTooltipBuilder(id: Int, anchorView: View): Tooltip.Builder {
        return Tooltip.Builder(id)
                .closePolicy(Tooltip.ClosePolicy().insidePolicy(true, false).outsidePolicy(true, false), 10000)
                .withArrow(true).withOverlay(false).fitToScreen(true).fadeDuration(250)
                .maxWidth(OTApplication.app.resources.getDimensionPixelSize(R.dimen.tooltip_max_width))
                .withStyleId(R.style.tooltipStyle)
                .anchor(anchorView, Tooltip.Gravity.TOP)
    }
}