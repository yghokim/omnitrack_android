package kr.ac.snu.hcil.omnitrack.core.fields.properties

import android.content.Context
import kr.ac.snu.hcil.android.common.containers.UniqueStringEntryList
import kr.ac.snu.hcil.omnitrack.core.R

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
            return UniqueStringEntryList.fromJson(serialized)
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

    override fun getSerializedValue(value: UniqueStringEntryList): String {
        return value.getSerializedString()
    }
}