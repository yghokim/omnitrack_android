package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import kr.ac.snu.hcil.omnitrack.ui.components.DateTimePicker

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
class TimePointInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<TimePoint>(R.layout.input_time_picker, context, attrs) {
    override val typeId: Int = VIEW_TYPE_TIME_POINT

    private lateinit var valueView: DateTimePicker

    override var value: TimePoint
        get() = valueView.time
        set(value) {
            valueView.time = value
        }

    init {
        valueView = findViewById(R.id.value) as DateTimePicker

        valueView.timeChanged += {
            sender, arg ->
            onValueChanged(arg)
        }
    }

    fun setPickerMode(mode: Int) {
        valueView.mode = mode
    }

    override fun focus() {
        valueView.requestFocus()
    }
}