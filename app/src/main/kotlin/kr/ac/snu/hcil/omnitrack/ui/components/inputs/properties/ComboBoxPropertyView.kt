package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.SpinnerAdapter
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by younghokim on 16. 8. 31..
 */
class ComboBoxPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<Int>(R.layout.component_property_combobox, context, attrs), AdapterView.OnItemSelectedListener {

    override var value: Int
        get() = spinner.selectedItemPosition
        set(value) {
            spinner.setSelection(value, true)
        }

    var adapter: SpinnerAdapter
        get() = spinner.adapter
        set(value) {
            spinner.adapter = value
        }

    private val spinner: Spinner

    init {
        spinner = findViewById(R.id.value) as Spinner
        spinner.setOnItemSelectedListener(this)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        onValueChanged(position)
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        spinner.setSelection(0)
    }

    override fun focus() {
    }
}