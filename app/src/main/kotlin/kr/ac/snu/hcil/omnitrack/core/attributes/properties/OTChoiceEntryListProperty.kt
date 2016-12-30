package kr.ac.snu.hcil.omnitrack.core.attributes.properties

import android.content.Context
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ChoiceEntryListPropertyView
import kr.ac.snu.hcil.omnitrack.utils.UniqueStringEntryList

/**
 * Created by younghokim on 16. 8. 13..
 */

class OTChoiceEntryListProperty(key: Int, title: String) : OTProperty<UniqueStringEntryList>(UniqueStringEntryList(PREVIEW_ENTRIES), key, title) {

    companion object {

        val PREVIEW_ENTRIES: Array<UniqueStringEntryList.Entry>

        init {
            PREVIEW_ENTRIES = OTApplication.app.resources.getStringArray(R.array.choice_preview_entries).mapIndexed {
                i, s ->
                UniqueStringEntryList.Entry(i, s)
            }.toTypedArray()
        }
    }

    override fun parseValue(serialized: String): UniqueStringEntryList {
        return UniqueStringEntryList(serialized)
    }

    override fun onBuildView(context: Context): APropertyView<UniqueStringEntryList> {
        return ChoiceEntryListPropertyView(context, null)
    }

    override fun getSerializedValue(): String {
        return value.getSerializedString()
    }


}