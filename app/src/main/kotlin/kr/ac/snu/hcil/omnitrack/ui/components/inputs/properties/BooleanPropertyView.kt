package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.support.v7.widget.SwitchCompat
import android.util.AttributeSet
import android.view.View
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by younghokim on 16. 8. 12..
 */
class BooleanPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<Boolean>(R.layout.component_property_boolean, context, attrs), View.OnClickListener {

    private val switch: SwitchCompat = findViewById(R.id.value)

    private val proxyButton: View = findViewById(R.id.ui_button_proxy)

    override var value: Boolean
        get() = switch.isChecked
        set(value) {
            switch.isChecked = value
        }

    init {
        proxyButton.setOnClickListener(this)
    }

    override fun focus() {
    }

    override fun onClick(view: View?) {
        if (validate(!value)) {
            switch.performClick()
            onValueChanged(switch.isChecked)
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