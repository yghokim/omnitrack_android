package kr.ac.snu.hcil.omnitrack.utils

import android.widget.Button
import android.widget.TextView

/**
 * Created by Young-Ho Kim on 2016-07-25.
 */
object InterfaceHelper {

    fun removeButtonTextDecoration(button: Button) {
        button.transformationMethod = null
        button.setAllCaps(false)
    }

    fun setTextAppearance(tv: TextView, style: Int) {
        if (android.os.Build.VERSION.SDK_INT < 23) {
            tv.setTextAppearance(tv.context, style)
        } else {
            tv.setTextAppearance(style)
        }

    }

    fun setTextAppearance(tv: Button, style: Int) {
        if (android.os.Build.VERSION.SDK_INT < 23) {
            tv.setTextAppearance(tv.context, style)
        } else {
            tv.setTextAppearance(style)
        }

    }
}