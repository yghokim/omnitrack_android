package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.util.AttributeSet
import android.widget.Adapter
import android.widget.ListAdapter
import com.jaredrummler.materialspinner.MaterialSpinner
import com.jaredrummler.materialspinner.MaterialSpinnerAdapter
import kotlinx.android.synthetic.main.component_property_spinner.view.*
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by younghokim on 16. 8. 31..
 */
class SpinnerPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<Any?>(R.layout.component_property_spinner, context, attrs), MaterialSpinner.OnItemSelectedListener<Any> {


    override var value: Any?
        get() = if (ui_value.selectedIndex == -1) null else ui_value.getItems<Any>()[ui_value.selectedIndex]
        set(value) {
            ui_value.selectedIndex = ui_value.getItems<Any>().indexOf(value)
        }

    var adapter: Adapter? = null
        private set

    init {
        ui_value.setOnItemSelectedListener(this)
    }

    fun setAdapter(adapter: ListAdapter) {
        this.adapter = adapter
        ui_value.setAdapter(adapter)
    }

    fun <T> setAdapter(adapter: MaterialSpinnerAdapter<T>) {
        this.adapter = adapter
        ui_value.setAdapter(adapter)
    }

    override fun onItemSelected(view: MaterialSpinner?, position: Int, id: Long, item: Any?) {
        onValueChanged(item)
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
        } catch (e: Exception) {
            return false
        }
    }
}