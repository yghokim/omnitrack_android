package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.TimeRangePicker

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
class TimeRangePickerInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<TimeSpan>(R.layout.input_time_range_picker, context, attrs) {
    override val typeId: Int = VIEW_TYPE_TIME_RANGE_PICKER

    private var valueView: TimeRangePicker = findViewById(R.id.value)

    override var value: TimeSpan? = TimeSpan()
        set(value) {
            //TODO Null UI
            if (field != value) {
                field = value
                valueView.setTimeSpan(value ?: TimeSpan())
            }
        }

    init {
        valueView.timeRangeChanged += {
            sender, timeSpan ->
            onValueChanged(timeSpan)
        }
    }

    override fun focus() {
        valueView.requestFocus()
    }

    fun setGranularity(value: TimeRangePicker.Granularity) {
        valueView.granularity = value
    }
}