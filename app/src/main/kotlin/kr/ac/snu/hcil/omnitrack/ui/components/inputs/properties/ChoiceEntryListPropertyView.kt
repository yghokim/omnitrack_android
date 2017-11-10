package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.common.choice.ChoiceEntryListEditor
import kr.ac.snu.hcil.omnitrack.utils.UniqueStringEntryList

/**
 * Created by younghokim on 16. 8. 13..
 */
class ChoiceEntryListPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<UniqueStringEntryList>(R.layout.component_property_choice_entry_list, context, attrs), ChoiceEntryListEditor.IListEditedListener {

    private val valueView: ChoiceEntryListEditor = findViewById(R.id.ui_value)

    private var cache: UniqueStringEntryList? = null

    override var value: UniqueStringEntryList
        get() {
            if (cache == null) {
                cache = valueView.getNotBlankEntryList()
            }
            return cache!!
        }
        set(value) {
            valueView.setEntryList(value)
        }

    init {
        useIntrinsicPadding = true

        valueView.addListEditedListener(this)
    }

    override fun onContentEdited(editor: ChoiceEntryListEditor) {
        cache = null
        onValueChanged(value)
    }

    override fun focus() {

    }


    override fun getSerializedValue(): String? {
        return value.getSerializedString()
    }

    override fun setSerializedValue(serialized: String): Boolean {
        try {
            value = UniqueStringEntryList(serialized)
            return true
        } catch(e: Exception) {
            return false
        }
    }


}