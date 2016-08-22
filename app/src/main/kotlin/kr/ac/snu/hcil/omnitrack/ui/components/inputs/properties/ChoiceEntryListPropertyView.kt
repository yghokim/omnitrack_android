package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.common.ChoiceEntryListEditor
import kr.ac.snu.hcil.omnitrack.utils.UniqueStringEntryList

/**
 * Created by younghokim on 16. 8. 13..
 */
class ChoiceEntryListPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<UniqueStringEntryList>(R.layout.component_property_choice_entry_list, context, attrs) {

    private val valueView: ChoiceEntryListEditor

    override var value: UniqueStringEntryList
        get() = valueView.getNotBlankEntryList()
        set(value) {
            valueView.setEntryList(value)
        }

    init {
        valueView = findViewById(R.id.value) as ChoiceEntryListEditor
        useIntrinsicPadding = true
    }


    override fun focus() {

    }


}