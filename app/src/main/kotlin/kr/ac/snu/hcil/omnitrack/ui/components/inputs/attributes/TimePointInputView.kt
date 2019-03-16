package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.types.TimePoint
import kr.ac.snu.hcil.omnitrack.views.time.DateTimePicker

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
class TimePointInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<TimePoint>(R.layout.input_time_picker, context, attrs) {
    override val typeId: Int = VIEW_TYPE_TIME_POINT

    private var valueView: DateTimePicker = findViewById(R.id.ui_value)

    override var value: TimePoint? = TimePoint()
        set(value) {
            if (field != value) {
                field = value
                //TODO null UI
                valueView.timeChanged.suspend = true
                valueView.time = value ?: TimePoint()
                valueView.timeChanged.suspend = false

                onValueChanged(value)
            }
        }

    init {
        valueView.timeChanged += {
            sender, arg ->
            value = arg
        }
    }

    fun setPickerMode(mode: Int) {
        valueView.mode = mode
    }

    override fun focus() {
        valueView.requestFocus()
    }
}