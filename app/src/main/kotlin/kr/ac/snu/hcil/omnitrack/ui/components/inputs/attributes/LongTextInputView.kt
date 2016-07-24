package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.util.AttributeSet
import android.widget.EditText
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by younghokim on 16. 7. 24..
 */
class LongTextInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<CharSequence>(R.layout.input_longtext, context, attrs) {
    override var value: CharSequence
        get() = valueView.text
        set(value) {
            valueView.setText(value, TextView.BufferType.EDITABLE)
        }

    override val typeId: Int = VIEW_TYPE_LONG_TEXT

    private lateinit var valueView: EditText

    init {
        valueView = findViewById(R.id.value) as EditText
    }

    override fun focus() {
        valueView.requestFocus()
    }


}