package kr.ac.snu.hcil.android.common.view.preference

import android.app.Dialog
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.core.os.bundleOf
import androidx.preference.DialogPreference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceViewHolder
import com.larswerkman.lobsterpicker.LobsterPicker
import com.larswerkman.lobsterpicker.adapters.BitmapColorAdapter
import com.larswerkman.lobsterpicker.sliders.LobsterShadeSlider
import kr.ac.snu.hcil.android.common.view.R
import mehdi.sakout.fancybuttons.FancyButton

/**
 * Created by younghokim on 2017. 5. 21..
 */
class ColorPreference : DialogPreference {

    internal var currentColor: Int = Color.WHITE
        set(value) {
            if (field != value) {
                field = value
                colorButton?.setBackgroundColor(value)
            }
        }


    internal fun setColor(color: Int) {
        currentColor = color
        persistInt(color)
    }

    private var colorButton: FancyButton? = null

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context, null)

    init {
        widgetLayoutResource = R.layout.layout_custom_preference_color
        dialogLayoutResource = R.layout.layout_dialog_color_preference

        setPositiveButtonText(android.R.string.ok)
        setNegativeButtonText(android.R.string.cancel)
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInteger(index, Color.WHITE)
    }


    override fun onSetInitialValue(defaultValue: Any?) {
        if (defaultValue != null) {
            currentColor = defaultValue as Int
            persistInt(currentColor)
        } else {
            currentColor = getPersistedInt(Color.WHITE)
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        colorButton = holder.findViewById(R.id.ui_button) as FancyButton
        colorButton?.setBackgroundColor(currentColor)
        colorButton?.setOnClickListener {
            onClick()
        }
    }
}

class ColorPreferenceDialogFragment : PreferenceDialogFragmentCompat() {

    companion object {
        const val KEY_PICKER_CURRENT_COLOR = "currentColor"

        fun makeInstance(key: String): ColorPreferenceDialogFragment =
                ColorPreferenceDialogFragment().apply {
                    arguments = bundleOf(ARG_KEY to key)
                }
    }

    private var colorPicker: LobsterPicker? = null
    private var colorSlider: LobsterShadeSlider? = null

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        colorPicker = view.findViewById(R.id.ui_color_picker)
        colorPicker?.colorAdapter = BitmapColorAdapter(context, R.drawable.lights_pallete)
        colorSlider = view.findViewById(R.id.ui_color_slider)

        if (colorSlider != null) {
            colorPicker?.addDecorator(colorSlider!!)
        }

        (preference as? ColorPreference)?.let {
            colorPicker?.color = it.currentColor
            colorPicker?.history = it.currentColor
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_PICKER_CURRENT_COLOR)) {
            colorPicker?.color = savedInstanceState.getInt(KEY_PICKER_CURRENT_COLOR)
        }

        return dialog
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        colorPicker?.let {
            outState.putInt(KEY_PICKER_CURRENT_COLOR, it.color)
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            colorPicker?.let {
                (preference as? ColorPreference)?.setColor(it.color)
            }
        }
    }

}