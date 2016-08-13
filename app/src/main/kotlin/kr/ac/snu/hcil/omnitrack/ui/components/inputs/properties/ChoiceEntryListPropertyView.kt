package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.ChoiceEntryListEditor

/**
 * Created by younghokim on 16. 8. 13..
 */
class ChoiceEntryListPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<Array<String>>(R.layout.component_property_choice_entry_list, context, attrs) {

    private val valueView: ChoiceEntryListEditor

    override var value: Array<String>
        get() = valueView.entries
        set(value) {
            valueView.entries = value
        }

    init {
        valueView = findViewById(R.id.value) as ChoiceEntryListEditor
        useIntrinsicPadding = true
    }


    override fun focus() {

    }


}