package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Switch
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by younghokim on 16. 8. 12..
 */
class BooleanPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<Boolean>(R.layout.component_property_boolean, context, attrs), View.OnClickListener {


    private val switch: Switch

    private val proxyButton: View

    override var value: Boolean
        get() = switch.isChecked
        set(value) {
            switch.isChecked = value
        }

    init {
        switch = findViewById(R.id.value) as Switch
        proxyButton = findViewById(R.id.ui_button_proxy)

        proxyButton.setOnClickListener(this)
    }

    override fun focus() {
    }

    override fun onClick(view: View?) {
        switch.performClick()
        onValueChanged(switch.isChecked)
    }

}