package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.util.AttributeSet
import android.widget.SpinnerAdapter
import kr.ac.snu.hcil.android.common.view.container.ExtendedSpinner
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by younghokim on 16. 8. 31..
 */
class ComboBoxPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<Int>(R.layout.component_property_combobox, context, attrs), ExtendedSpinner.OnItemSelectedListener {

    override var value: Int
        get() = spinner.selectedItemPosition
        set(value) {
            spinner.selectedItemPosition = value
        }

    var adapter: SpinnerAdapter?
        get() = spinner.adapter
        set(value) {
            spinner.adapter = value
        }

    private val spinner: ExtendedSpinner = findViewById(R.id.ui_value)

    init {
        spinner.onItemSelectedListener = this
    }

    override fun onItemSelected(spinner: ExtendedSpinner, position: Int) {
        onValueChanged(position)
    }

    override fun focus() {
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