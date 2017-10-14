package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.EditText
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.EnterHideKeyboardEditorActionListener

/**
 * Created by Young-Ho Kim on 2016-08-01.
 */
abstract class ACharSequenceAttributeInputView(layoutId: Int, context: Context, attrs: AttributeSet? = null) : AAttributeInputView<CharSequence>(layoutId, context, attrs) {
    override var value: CharSequence? = null
        set(rawValue) {
            val value: String? = if (rawValue?.isNotBlank() == true) rawValue.toString().trimEnd() else null
            println("TextView comparison. original: ${field} | new: ${value} | isDifferent: ${field != value}")
            if (field != value) {
                field = value
                if (valueView.text.toString() != value || !(valueView.text.toString().isBlank() && value.isNullOrBlank())) {
                    valueView.setText(value, TextView.BufferType.EDITABLE)
                    if (value != null && !value.isNullOrBlank())
                        valueView.setSelection(value.length)
                }
                onValueChanged(value)
            }

        }

    private val valueView: EditText = findViewById(R.id.value)

    init {
        valueView.setOnEditorActionListener(EditorActionListener())
        valueView.setOnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                value = valueView.text.toString().trimEnd()
            }
        }
    }

    override fun focus() {
        valueView.requestFocus()
    }

    override fun clearFocus() {
        valueView.clearFocus()
    }

    inner class EditorActionListener : EnterHideKeyboardEditorActionListener() {
        override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
            val superResult = super.onEditorAction(v, actionId, event)

            if (superResult) {
                //pressed enter
                value = v.text.toString()
            }

            return superResult
        }
    }
}