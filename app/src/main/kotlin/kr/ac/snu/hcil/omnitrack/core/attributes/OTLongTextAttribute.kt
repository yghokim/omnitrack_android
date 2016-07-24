package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.LongTextInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.NumberInputView

/**
 * Created by younghokim on 16. 7. 24..
 */
class OTLongTextAttribute(objectId: String?, dbId: Long?, columnName: String, settingData: String?) : OTAttribute<CharSequence>(objectId, dbId, columnName, Companion.TYPE_LONG_TEXT, settingData) {

    override val keys: Array<Int>
        get() = Array<Int>(0) { index -> 0 }

    override val typeNameResourceId: Int
        get() = R.string.type_longtext_name

    override fun createProperties() {
    }

    override fun parseAttributeValue(storedValue: String): CharSequence {
        return storedValue
    }

    override fun formatAttributeValue(value: Any): String {
        return value.toString()
    }

    override fun makeDefaultValue(): CharSequence {
        return ""
    }

    override fun getInputView(context: Context, recycledView: AAttributeInputView<out Any>?): AAttributeInputView<out Any> {
        val view =
                if ((recycledView?.typeId == AAttributeInputView.VIEW_TYPE_LONG_TEXT)) {
                    recycledView!! as LongTextInputView
                } else {
                    LongTextInputView(context)
                }

        view.value = makeDefaultValue()

        return view
    }
}