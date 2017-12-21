package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.util.AttributeSet
import android.view.View
import kotlinx.android.synthetic.main.component_property_toggle_lottie.view.*
import kr.ac.snu.hcil.omnitrack.R
import org.jetbrains.anko.backgroundResource

/**
 * Created by younghokim on 2017. 11. 10..
 */
class ToggleAnimPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<Boolean>(R.layout.component_property_toggle_lottie, context, attrs), View.OnClickListener {
    override var value: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                setToggleMode(value, true, true)
                onValueChanged(value)
            }
        }

    init {

        val a = context.theme.obtainStyledAttributes(
                attrs, intArrayOf(R.attr.lottie_fileName),
                0, 0)

        try {
            if (a.hasValue(0)) {
                val filePath = a.getString(0)
                ui_lottie_view.setAnimation(filePath)
            }
        } finally {
            a.recycle()
        }

        this.setOnClickListener(this)

        if (this.background == null) {
            this.backgroundResource = R.drawable.transparent_button_background
        }
    }

    override fun focus() {
    }

    override fun onClick(view: View?) {
        if (validate(!value)) {
            value = !value
        }
    }

    fun setToggleMode(isOn: Boolean, animate: Boolean, force: Boolean = false) {
        if (value != isOn || force) {
            value = isOn
            if (/*animate == false*/true) {
                if (ui_lottie_view.isAnimating) ui_lottie_view.cancelAnimation()

                ui_lottie_view.progress = if (isOn) 0.5f else 0f
            } else {
                if (isOn) {
                    ui_lottie_view.playAnimation(0.0f, 0.5f)
                } else {
                    ui_lottie_view.reverseAnimation()
                    ui_lottie_view.playAnimation(0.5f, 0.0f)
                }
            }
        }
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