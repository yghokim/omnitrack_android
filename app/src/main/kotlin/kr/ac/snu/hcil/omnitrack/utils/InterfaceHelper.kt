package kr.ac.snu.hcil.omnitrack.utils

import android.widget.Button

/**
 * Created by Young-Ho Kim on 2016-07-25.
 */
object InterfaceHelper {

    fun removeButtonTextDecoration(button: Button) {
        button.transformationMethod = null
        button.setAllCaps(false)
    }
}