package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho on 8/25/2016.
 */
class HourRangePicker : LinearLayout {

    var fromHourOfDay: Int
        get() = fromPicker.hourOfDay
        set(value) {
            fromPicker.hourOfDay = value
        }

    var toHourOfDay: Int
        get() = toPicker.hourOfDay
        set(value) {
            toPicker.hourOfDay = value
        }

    private val fromPicker: HourPicker
    private val toPicker: HourPicker


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {

        orientation = HORIZONTAL

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.component_hour_range_picker, this, true)

        fromPicker = findViewById(R.id.ui_picker_from) as HourPicker
        toPicker = findViewById(R.id.ui_picker_to) as HourPicker

    }

}