package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.support.v7.widget.AppCompatButton
import android.util.AttributeSet

/**
 * Created by younghokim on 2017. 1. 13..
 */
open class DrawableTintAppCompatButton : AppCompatButton {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val core = CompoundDrawableTintCore()
        val cd = core.init(context, compoundDrawablesRelative, attrs, 0)
        super.setCompoundDrawablesRelativeWithIntrinsicBounds(cd[0], cd[1], cd[2], cd[3])
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val core = CompoundDrawableTintCore()
        val cd = core.init(context, compoundDrawablesRelative, attrs, 0)
        super.setCompoundDrawablesRelativeWithIntrinsicBounds(cd[0], cd[1], cd[2], cd[3])
    }
}