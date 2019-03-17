package kr.ac.snu.hcil.omnitrack.views.properties

import android.content.Context
import android.util.AttributeSet
import android.view.View
import kotlinx.android.synthetic.main.component_property_boolean.view.*

/**
 * Created by younghokim on 16. 8. 12..
 */
class BooleanPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<Boolean>(R.layout.component_property_boolean, context, attrs), View.OnClickListener {

    override var value: Boolean
        get() = ui_value.isChecked
        set(value) {
            ui_value.isChecked = value
        }

    init {
        ui_button_proxy.setOnClickListener(this)
    }

    override fun focus() {
    }

    override fun onClick(view: View?) {
        if (validate(!value)) {
            ui_value.performClick()
            onValueChanged(ui_value.isChecked)
        }
    }

    override fun getSerializedValue(): String? {
        return value.toString()
    }

    override fun setSerializedValue(serialized: String): Boolean {
        try {
            value = serialized.toBoolean()
            return true
        } catch(e: Exception) {
            return false
        }
    }

}