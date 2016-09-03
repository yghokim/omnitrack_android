package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView

/**
 * Created by Young-Ho Kim on 2016-07-13.
 */
class ShortTextPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<String>(R.layout.component_property_shorttext, context, attrs) {

    companion object{
         val NOT_EMPTY_VALIDATOR : ((String)->Boolean) = { it != "" }

    }

    override var value: String
        get() = valueView.text.toString()
        set(value) {
            if(valueView.text != value) {
                valueView.text = value
            }
        }

    lateinit private var valueView : TextView

    init{
        valueView = findViewById(R.id.value) as TextView
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
    }

    override fun focus() {
        valueView.requestFocus()
    }

    override fun onValidated(result: Boolean)
    {
        super.onValidated(result)
        if(result == false)
        {
            valueView.error = validationErrorMessageList.joinToString("\n")
        }
        else{
            valueView.error = null
        }
    }

}