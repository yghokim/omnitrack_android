package kr.ac.snu.hcil.omnitrack.core.attributes.properties

import android.content.Context
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.SelectionPropertyView

/**
 * Created by younghokim on 16. 7. 12..
 */
class OTSelectionPropertyHelper : OTPropertyHelper<Int>() {
    override fun getSerializedValue(value: Int): String {
        return value.toString()
    }

    override fun parseValue(serialized: String): Int {
        return serialized.toInt()
    }

    override fun makeView(context: Context): APropertyView<Int> {
        val result = SelectionPropertyView(context, null)
        //result.setEntries(entries)
        return result
    }

}