package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho on 8/7/2016.
 */
class TimeRangePicker : FrameLayout {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        addView(inflater.inflate(R.layout.component_time_range_picker, this, false))
    }
}