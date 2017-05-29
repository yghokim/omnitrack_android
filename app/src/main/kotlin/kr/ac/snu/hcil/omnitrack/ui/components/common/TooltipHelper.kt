package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.view.View
import it.sephiroth.android.library.tooltip.TooltipManager
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-11-03.
 */
object TooltipHelper {
    fun makeTooltipBuilder(id: Int, anchorView: View): TooltipManager.Builder {
        return TooltipManager.getInstance()
                .create(id)
                .closePolicy(TooltipManager.ClosePolicy.TouchOutside, 3000)
                .fitToScreen(true).fadeDuration(250)
                .activateDelay(200)
                .background("#af000000")
                .maxWidth(OTApplication.app.resourcesWrapped.getDimensionPixelSize(R.dimen.tooltip_max_width))
                .withStyleId(R.style.tooltipStyle)
                .anchor(anchorView, TooltipManager.Gravity.TOP)
    }
}