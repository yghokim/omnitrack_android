package kr.ac.snu.hcil.omnitrack.core.attributes.properties

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ChoiceEntryListPropertyView
import kr.ac.snu.hcil.omnitrack.utils.UniqueStringEntryList

/**
 * Created by younghokim on 16. 8. 13..
 */

class OTChoiceEntryListPropertyHelper(val context: Context) : OTPropertyHelper<UniqueStringEntryList>() {

    val previewChoiceEntries: Array<UniqueStringEntryList.Entry> by lazy {
        context.resources.getStringArray(R.array.choice_preview_entries).mapIndexed { i, s ->
            UniqueStringEntryList.Entry(i, s)
        }.toTypedArray()
    }

    override fun parseValue(serialized: String): UniqueStringEntryList {
        try {
            return UniqueStringEntryList.parser.fromJson(serialized, UniqueStringEntryList::class.java)
        } catch (e: Exception) {
            println("UniqueStringEntryList parse error")
            println(e)
            try {
                return UniqueStringEntryList(serialized)
            } catch (e2: Exception) {
                return UniqueStringEntryList(previewChoiceEntries)
            }
        }
    }

    override fun makeView(context: Context): APropertyView<UniqueStringEntryList> {
        return ChoiceEntryListPropertyView(context, null)
    }

    override fun getSerializedValue(value: UniqueStringEntryList): String {
        return value.getSerializedString()
    }
}