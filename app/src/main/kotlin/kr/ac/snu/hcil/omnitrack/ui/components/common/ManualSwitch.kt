package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.support.v7.widget.SwitchCompat
import android.util.AttributeSet

/**
 * Created by Young-Ho Kim on 2016-08-12.
 */
class ManualSwitch : SwitchCompat {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        isClickable = false
    }

    override fun toggle() {
        ;
    }
}