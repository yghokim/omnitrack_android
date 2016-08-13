package kr.ac.snu.hcil.omnitrack.core.attributes.properties

import android.content.Context
import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ChoiceEntryListPropertyView

/**
 * Created by younghokim on 16. 8. 13..
 */
class OTChoiceEntryListProperty(key: Int, title: String) : OTProperty<Array<String>>(arrayOf(), key, title) {

    override fun parseValue(serialized: String): Array<String> {
        return Gson().fromJson(serialized, Array<String>::class.java)
    }

    override fun onBuildView(context: Context): APropertyView<Array<String>> {
        return ChoiceEntryListPropertyView(context, null)
    }

    override fun getSerializedValue(): String {
        return Gson().toJson(value)
    }


}