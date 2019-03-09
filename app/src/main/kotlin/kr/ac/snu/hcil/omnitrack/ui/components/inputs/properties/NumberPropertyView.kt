package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.widget.TextView
import kr.ac.snu.hcil.android.common.view.text.EnterHideKeyboardEditorActionListener
import kr.ac.snu.hcil.omnitrack.R
import java.math.BigDecimal

/**
 * Created by younghokim on 2018. 1. 22..
 */
class NumberPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<BigDecimal>(R.layout.component_property_shorttext, context, attrs) {
    override var value: BigDecimal
        get() = BigDecimal(valueView.text.toString())
        set(value) {
            valueView.text = value.toPlainString()
        }

    override fun getSerializedValue(): String? {
        return value.toString()
    }

    override fun setSerializedValue(serialized: String): Boolean {
        try {
            this.value = BigDecimal(serialized)
            return true
        } catch (ex: Exception) {
            ex.printStackTrace()
            return false
        }
    }

    override fun focus() {
        valueView.requestFocus()
    }


    private var valueView: TextView = findViewById(R.id.ui_value)

    init {
        valueView.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NULL
        valueView.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                validate(BigDecimal(s.toString()))
                onValueChanged(BigDecimal(s.toString()))
            }
        })

        valueView.setOnEditorActionListener(EnterHideKeyboardEditorActionListener())
    }
}