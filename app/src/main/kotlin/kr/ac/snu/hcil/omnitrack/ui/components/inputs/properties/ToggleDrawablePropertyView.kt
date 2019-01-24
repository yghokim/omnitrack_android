package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.util.AttributeSet
import android.view.View
import kotlinx.android.synthetic.main.component_property_toggle_drawable.view.*
import kr.ac.snu.hcil.omnitrack.R
import org.jetbrains.anko.backgroundResource

/**
 * Created by younghokim on 2017. 11. 10..
 */
class ToggleDrawablePropertyView(context: Context, attrs: AttributeSet?) : APropertyView<Boolean>(R.layout.component_property_toggle_drawable, context, attrs), View.OnClickListener {
    override var value: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                setToggleMode(value)
                onValueChanged(value)
            }
        }

    init {

        val a = context.theme.obtainStyledAttributes(
                attrs, intArrayOf(R.attr.srcCompat),
                0, 0)

        try {
            if (a.hasValue(0)) {
                val toggleDrawable = a.getDrawable(0)
                if (toggleDrawable != null) {
                    ui_image_view.setImageDrawable(toggleDrawable)
                }
            }
        } finally {
            a.recycle()
        }

        this.setOnClickListener(this)

        if (this.background == null) {
            this.backgroundResource = R.drawable.transparent_button_background
        }

        ui_image_view.isActivated = value
    }

    override fun focus() {
    }

    override fun onClick(view: View?) {
        if (validate(!value)) {
            value = !value
        }
    }

    private fun setToggleMode(isOn: Boolean) {
        ui_image_view.isActivated = isOn
    }

    override fun getSerializedValue(): String? {
        return value.toString()
    }

    override fun setSerializedValue(serialized: String): Boolean {
        try {
            value = serialized.toBoolean()
            return true
        } catch (e: Exception) {
            return false
        }
    }
}