package kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.factories

import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTNumberFieldHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.types.NumberStyle
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.OTFieldViewFactory
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.NumberInputView
import java.math.BigDecimal

class NumberViewFactory(helper: OTNumberFieldHelper) : OTFieldViewFactory<OTNumberFieldHelper>(helper) {

    override fun getInputViewType(previewMode: Boolean, field: OTFieldDAO): Int = AFieldInputView.VIEW_TYPE_NUMBER

    override fun refreshInputViewUI(inputView: AFieldInputView<out Any>, field: OTFieldDAO) {
        if (inputView is NumberInputView) {
            inputView.numberStyle = helper.getNumberStyle(field) ?: NumberStyle()
            inputView.moveUnit = helper.getDeserializedPropertyValue<BigDecimal>(OTNumberFieldHelper.BUTTON_UNIT, field)
                    ?: BigDecimal.ONE
        }
    }
}