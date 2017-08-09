package kr.ac.snu.hcil.omnitrack.ui.components.common.viewholders

import android.view.View
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by younghokim on 2016. 11. 30..
 */
open class SimpleNameDescriptionViewHolder(val view: View) {

    val nameView: TextView = view.findViewById(R.id.name)
    val descriptionView: TextView = view.findViewById(R.id.description)

    var name: CharSequence
        get() = nameView.text.toString()
        set(value) {
            nameView.text = value
        }

    var description: CharSequence
        get() = descriptionView.text.toString()
        set(value) {
            descriptionView.text = value
        }

}