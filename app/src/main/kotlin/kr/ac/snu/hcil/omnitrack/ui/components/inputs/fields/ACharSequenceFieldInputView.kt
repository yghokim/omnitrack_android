package kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.EditText
import android.widget.TextView
import kr.ac.snu.hcil.android.common.view.text.EnterHideKeyboardEditorActionListener
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-08-01.
 */
abstract class ACharSequenceFieldInputView(layoutId: Int, context: Context, attrs: AttributeSet? = null) : AFieldInputView<CharSequence>(layoutId, context, attrs) {
    override var value: CharSequence? = null
        set(rawValue) {
            val value: String? = if (rawValue?.isNotBlank() == true) rawValue.toString().trimEnd() else null
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

    private val valueView: EditText = findViewById(R.id.ui_value)

    init {
        valueView.setOnEditorActionListener(EditorActionListener())
        valueView.setOnFocusChangeListener { _, hasFocus ->
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