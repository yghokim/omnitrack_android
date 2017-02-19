package kr.ac.snu.hcil.omnitrack.core.attributes.properties

import android.content.Context
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.SelectionPropertyView

/**
 * Created by younghokim on 16. 7. 12..
 */
class OTSelectionProperty(key: String, title: String, private val entries: Array<String>) : OTProperty<Int>(0, key, title) {
    override fun getSerializedValue(): String {
        return value.toString()
    }

    override fun parseValue(serialized: String): Int {
        return serialized.toInt()
    }

    override fun onBuildView(context: Context): APropertyView<Int> {
        val result = SelectionPropertyView(context, null)
        result.setEntries(entries)
        return result
    }

}