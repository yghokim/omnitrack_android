package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.inflateContent


/**
 * Created by younghokim on 2017. 4. 18..
 */
open class StringIndicatorPropertyView : LinearLayout {

    private val indicatorView: TextView
    private val titleView: TextView

    var title: String
        get() {
            return titleView.text.toString()
        }
        set(value) {
            titleView.text = value
        }

    var indicator: CharSequence
        get() {
            return indicatorView.text
        }
        set(value) {
            indicatorView.text = value
        }


    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    init {
        val attrs = intArrayOf(R.attr.selectableItemBackground)
        val typedArray = context.obtainStyledAttributes(attrs)
        val backgroundResource = typedArray.getResourceId(0, 0)
        setBackgroundResource(backgroundResource)

        minimumHeight = resources.getDimensionPixelSize(R.dimen.button_height_tall)

        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)

        setPadding(horizontalPadding, 0, horizontalPadding, 0)

        inflateContent(R.layout.component_string_indicator_property, true)

        indicatorView = findViewById(R.id.ui_indicator)
        titleView = findViewById(R.id.ui_title)

        typedArray.recycle()
    }

    private fun init(context: Context, attrs: AttributeSet?) {
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