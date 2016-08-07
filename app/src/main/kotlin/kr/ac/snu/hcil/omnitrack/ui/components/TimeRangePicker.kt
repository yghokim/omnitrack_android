package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper

/**
 * Created by Young-Ho on 8/7/2016.
 */
class TimeRangePicker : FrameLayout {

    private lateinit var fromButton: Button
    private lateinit var toButton: Button
    private lateinit var durationIndicator: TextView

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        addView(inflater.inflate(R.layout.component_time_range_picker, this, false))

        fromButton = findViewById(R.id.ui_button_from) as Button
        toButton = findViewById(R.id.ui_button_to) as Button
        durationIndicator = findViewById(R.id.ui_interval_indicator) as TextView

        InterfaceHelper.removeButtonTextDecoration(fromButton)
        InterfaceHelper.removeButtonTextDecoration(toButton)

    }
}