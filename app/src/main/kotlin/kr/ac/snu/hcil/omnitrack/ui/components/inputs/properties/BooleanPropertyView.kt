package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.util.AttributeSet
import android.widget.Switch
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by younghokim on 16. 8. 12..
 */
class BooleanPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<Boolean>(R.layout.component_property_boolean, context, attrs) {

    private var switch: Switch

    override var value: Boolean
        get() = switch.isChecked
        set(value) {
            switch.isChecked = value
        }

    init {
        switch = findViewById(R.id.value) as Switch

        switch.setOnClickListener {
            onValueChanged(switch.isEnabled)
        }
    }

    override fun focus() {
    }

}