package kr.ac.snu.hcil.omnitrack.core.attributes.properties

import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTProperty
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.SelectionPropertyView

/**
 * Created by younghokim on 16. 7. 12..
 */
class OTSelectionProperty(key: Int, title: String, private val values: Array<String>) : OTProperty<Int>(0, key, title) {
    override fun getSerializedValue(): String {
        return value.toString()
    }

    override fun parseValue(serialized: String): Int {
        return serialized.toInt()
    }

    override fun buildView(context: Context): APropertyView<Int> {
        val result = SelectionPropertyView(context, null)
        result.setValues(values)
        result.title = title
        return result
    }

}