package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-07-13.
 */
abstract class APropertyView<T>(layoutId: Int, context: Context, attrs: AttributeSet?) : AInputView<T>(layoutId, context, attrs) {

    protected var titleView: TextView

    var useIntrinsicPadding: Boolean = false

    var title: CharSequence
        get() = titleView.text
        set(value) {
            titleView.text = value
        }

    constructor(layoutId: Int, context: Context) : this(layoutId, context, null)

    init {

        titleView = findViewById(R.id.title) as TextView

        val a = context.theme.obtainStyledAttributes(
                attrs, intArrayOf(android.R.attr.text),
                0, 0)

        try {
            if (a.hasValue(0))
                title = a.getString(0)
        } finally {
            a.recycle()
        }

    }
}