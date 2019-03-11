package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.factories

import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTNumberAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.types.NumberStyle
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AttributeViewFactory
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.NumberInputView
import java.math.BigDecimal

class NumberViewFactory(helper: OTNumberAttributeHelper) : AttributeViewFactory<OTNumberAttributeHelper>(helper) {

    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int = AAttributeInputView.VIEW_TYPE_NUMBER

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>, attribute: OTAttributeDAO) {
        if (inputView is NumberInputView) {
            inputView.numberStyle = helper.getNumberStyle(attribute) ?: NumberStyle()
            inputView.moveUnit = helper.getDeserializedPropertyValue<BigDecimal>(OTNumberAttributeHelper.BUTTON_UNIT, attribute)
                    ?: BigDecimal.ONE
        }
    }
}