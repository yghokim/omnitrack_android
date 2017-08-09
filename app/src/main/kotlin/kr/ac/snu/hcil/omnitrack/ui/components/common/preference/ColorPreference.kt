package kr.ac.snu.hcil.omnitrack.ui.components.common.preference

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import com.larswerkman.lobsterpicker.LobsterPicker
import com.larswerkman.lobsterpicker.adapters.BitmapColorAdapter
import com.larswerkman.lobsterpicker.sliders.LobsterShadeSlider
import kr.ac.snu.hcil.omnitrack.R
import mehdi.sakout.fancybuttons.FancyButton

/**
 * Created by younghokim on 2017. 5. 21..
 */
class ColorPreference : DialogPreference {

    private var currentColor: Int = Color.WHITE
        set(value) {
            if (field != value) {
                field = value
                colorButton?.setBackgroundColor(value)
            }
        }

    private var colorButton: FancyButton? = null

    private var colorPicker: LobsterPicker? = null
    private var colorSlider: LobsterShadeSlider? = null

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    init {
        widgetLayoutResource = R.layout.layout_custom_preference_color
        dialogLayoutResource = R.layout.layout_dialog_color_preference

        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }

    override fun onBindDialogView(view: View) {

        colorPicker = view.findViewById(R.id.ui_color_picker)
        colorPicker?.colorAdapter = BitmapColorAdapter(context, R.drawable.lights_pallete)
        colorSlider = view.findViewById(R.id.ui_color_slider)

        if (colorSlider != null) {
            colorPicker?.addDecorator(colorSlider!!)
        }

        colorPicker?.color = currentColor
        colorPicker?.history = currentColor

        super.onBindDialogView(view)
    }

    override fun onDialogClosed(positiveResult: Boolean) {

        if (positiveResult) {
            colorPicker?.let {
                currentColor = it.color
                persistInt(currentColor)
            }
        }

        super.onDialogClosed(positiveResult)
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInteger(index, Color.WHITE)
    }

    override fun onBindView(view: View) {
        super.onBindView(view)

        colorButton = view.findViewById(R.id.ui_button)
        colorButton?.setBackgroundColor(currentColor)
        colorButton?.setOnClickListener {
            onClick()
        }
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        if (restorePersistedValue) {
            currentColor = getPersistedInt(Color.WHITE)
        } else {
            currentColor = defaultValue as Int
            persistInt(currentColor)
        }
    }
}