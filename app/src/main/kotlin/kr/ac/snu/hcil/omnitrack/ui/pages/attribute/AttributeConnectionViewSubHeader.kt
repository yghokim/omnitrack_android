package kr.ac.snu.hcil.omnitrack.ui.pages.attribute

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho on 8/11/2016.
 */
class AttributeConnectionViewSubHeader : LinearLayout {

    private var iconView: ImageView
    private var labelView: TextView


    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }


    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    init {
        orientation = HORIZONTAL

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.component_attribute_connection_subheader, this, true)

        iconView = findViewById(R.id.ui_icon) as ImageView
        labelView = findViewById(R.id.ui_label) as TextView
    }

    private fun init(context: Context, attrs: AttributeSet) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.AttributeConnectionViewSubHeader, 0, 0)
        try {

            val iconResourceId = a.getResourceId(R.styleable.AttributeConnectionViewSubHeader_icon, R.drawable.link_dark)
            val label = a.getString(R.styleable.AttributeConnectionViewSubHeader_label)

            iconView.setImageResource(iconResourceId)
            labelView.text = label
        } finally {
            a.recycle()
        }
    }


}