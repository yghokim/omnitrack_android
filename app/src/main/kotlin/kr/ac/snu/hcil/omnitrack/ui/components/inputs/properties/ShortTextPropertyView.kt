package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.widget.TextView
import kr.ac.snu.hcil.android.common.view.text.EnterHideKeyboardEditorActionListener
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-07-13.
 */
class ShortTextPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<String>(R.layout.component_property_shorttext, context, attrs) {

    companion object {
        val NOT_EMPTY_VALIDATOR: ((String) -> Boolean) = { it != "" }

    }

    override var value: String
        get() = valueView.text.toString()
        set(value) {
            if (valueView.text.toString() != value) {
                valueView.text = value
            }
        }

    private var valueView: TextView = findViewById(R.id.ui_value)

    init {
        valueView.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                validate(s.toString())
                onValueChanged(s.toString())
            }
        })

        valueView.setOnEditorActionListener(EnterHideKeyboardEditorActionListener())
    }

    override fun focus() {
        valueView.requestFocus()
    }

    override fun onValidated(result: Boolean) {
        super.onValidated(result)
        if (!result) {
            valueView.error = validationErrorMessageList.joinToString("\n")
        } else {
            valueView.error = null
        }
    }

    override fun getSerializedValue(): String? {
        return value
    }

    override fun setSerializedValue(serialized: String): Boolean {
        try {
            value = serialized
            return true
        } catch(e: Exception) {
            return false
        }
    }
}