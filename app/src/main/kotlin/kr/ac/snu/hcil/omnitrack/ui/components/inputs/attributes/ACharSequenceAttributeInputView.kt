package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.util.AttributeSet
import android.widget.EditText
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-08-01.
 */
abstract class ACharSequenceAttributeInputView(layoutId: Int, context: Context, attrs: AttributeSet? = null) : AAttributeInputView<CharSequence>(layoutId, context, attrs) {
    override var value: CharSequence
        get() = valueView.text
        set(value) {
            if (value != valueView.text.toString()) {
                valueView.setText(value, TextView.BufferType.EDITABLE)
                onValueChanged(value)
            }

        }

    private lateinit var valueView: EditText

    init {
        valueView = findViewById(R.id.value) as EditText
    }

    override fun focus() {
        valueView.requestFocus()
    }
}