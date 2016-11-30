package kr.ac.snu.hcil.omnitrack.ui.components.common.viewholders

import android.view.View
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by younghokim on 2016. 11. 30..
 */
class SimpleNameDescriptionViewHolder(val view: View) {

    val nameView: TextView
    val descriptionView: TextView

    init {
        descriptionView = view.findViewById(R.id.description) as TextView
        nameView = view.findViewById(R.id.name) as TextView
    }
}