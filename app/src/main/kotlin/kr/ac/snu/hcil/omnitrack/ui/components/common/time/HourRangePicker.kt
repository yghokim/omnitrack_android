package kr.ac.snu.hcil.omnitrack.ui.components.common.time

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.events.Event

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

    val onRangeChanged = Event<Pair<Int, Int>>()


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {

        orientation = HORIZONTAL

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.component_hour_range_picker, this, true)

        fromPicker = findViewById(R.id.ui_picker_from)
        toPicker = findViewById(R.id.ui_picker_to)

        val handler = { sender: Any, hourOfDay: Int ->
            onRangeChanged.invoke(this, Pair(fromHourOfDay, toHourOfDay))
        }

        fromPicker.hourOfDayChanged += handler

        toPicker.hourOfDayChanged += handler
    }

}