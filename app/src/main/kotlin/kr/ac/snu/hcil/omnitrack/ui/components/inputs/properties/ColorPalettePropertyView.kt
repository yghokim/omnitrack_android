package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.common.ColorPaletteView

/**
 * Created by Young-Ho Kim on 2016-07-20.
 */
class ColorPalettePropertyView(context: Context, attrs: AttributeSet?) : APropertyView<Int>(R.layout.component_property_color_palette, context, attrs) {

    private var paletteView: ColorPaletteView

    override var value: Int
        get() = paletteView.selectedColor
        set(value) {
            paletteView.selectedColor = value
        }

    override fun focus() {

    }

    init {
        paletteView = findViewById(R.id.value) as ColorPaletteView
        paletteView.colorChanged += {
            sender, value ->
            onValueChanged(value)
        }
    }


    override fun getSerializedValue(): String? {
        return value.toString()
    }

    override fun setSerializedValue(serialized: String): Boolean {
        try {
            value = serialized.toInt()
            return true
        } catch(e: Exception) {
            return false
        }
    }
}