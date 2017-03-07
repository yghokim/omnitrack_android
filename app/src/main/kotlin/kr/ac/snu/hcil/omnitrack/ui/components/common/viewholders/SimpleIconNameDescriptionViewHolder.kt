package kr.ac.snu.hcil.omnitrack.ui.components.common.viewholders

import android.view.View
import android.widget.ImageView
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by younghokim on 2017. 3. 6..
 */
open class SimpleIconNameDescriptionViewHolder(view: View) : SimpleNameDescriptionViewHolder(view) {
    val iconView: ImageView = view.findViewById(R.id.icon) as ImageView

    fun setIconDrawable(res: Int) {
        iconView.setImageResource(res)
    }
}