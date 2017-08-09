package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.widget.EditText
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.EnterHideKeyboardEditorActionListener

/**
 * Created by Young-Ho Kim on 2016-08-01.
 */
abstract class ACharSequenceAttributeInputView(layoutId: Int, context: Context, attrs: AttributeSet? = null) : AAttributeInputView<CharSequence>(layoutId, context, attrs) {
    override var value: CharSequence
        get() = valueView.text.toString()
        set(value) {
            if (value != valueView.text.toString()) {
                valueView.setText(value, TextView.BufferType.EDITABLE)
            }
        }

    private val valueView: EditText = findViewById(R.id.value)

    init {
        valueView.addTextChangedListener(Watcher())

        EnterHideKeyboardEditorActionListener(valueView)
    }

    override fun focus() {
        valueView.requestFocus()
    }

    inner class Watcher : TextWatcher {
        override fun afterTextChanged(editable: Editable) {
            onValueChanged(editable.toString())
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

    }
}