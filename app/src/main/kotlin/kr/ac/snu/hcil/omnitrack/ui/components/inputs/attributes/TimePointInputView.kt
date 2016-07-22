package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.util.AttributeSet
import android.widget.EditText
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
class TimePointInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<TimePoint>(R.layout.input_timepoint, context, attrs) {
    override val typeId: Int = TYPE_TIME_POINT

    private lateinit var valueView: EditText

    override var value: TimePoint = TimePoint()
        get() = TimePoint()
        set(value) {
            field = value
        }

    init {
        valueView = findViewById(R.id.value) as EditText
    }

    override fun focus() {
        valueView.requestFocus()
    }
}