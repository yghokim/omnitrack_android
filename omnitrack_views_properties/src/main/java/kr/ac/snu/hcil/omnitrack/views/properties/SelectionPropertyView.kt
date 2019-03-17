package kr.ac.snu.hcil.omnitrack.views.properties

import android.content.Context
import android.util.AttributeSet
import kotlinx.android.synthetic.main.component_property_selection.view.*

/**
 * Created by Young-Ho Kim on 2016-07-20.
 */
class SelectionPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<Int>(R.layout.component_property_selection, context, attrs) {

    override var value: Int
        get() = ui_value.selectedIndex
        set(value) {
            ui_value.selectedIndex = value
        }

    override fun focus() {

    }

    init {
        ui_value.onSelectedIndexChanged += {
            sender, index ->
            onValueChanged(index)
        }
    }

    fun setEntries(values: Array<String>) {
        ui_value.setValues(values)
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