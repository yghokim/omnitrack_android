package kr.ac.snu.hcil.omnitrack.ui.components.properties

import android.content.Context
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.ColorPaletteView

/**
 * Created by Young-Ho Kim on 2016-07-20.
 */
class ColorPalettePropertyView(context: Context, attrs: AttributeSet?) : APropertyView<Int>(R.layout.component_property_color_palette, context, attrs) {

    private lateinit var paletteView: ColorPaletteView

    override var value: Int
        get() = paletteView.selectedColor
        set(value) {
            paletteView.selectedColor = value
        }

    override fun focus() {

    }

    init {
        paletteView = findViewById(R.id.value) as ColorPaletteView
    }


}