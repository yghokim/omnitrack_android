package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.factories

import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTShortTextAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AttributeViewFactory

class ShortTextViewFactory(helper: OTShortTextAttributeHelper) : AttributeViewFactory<OTShortTextAttributeHelper>(helper) {
    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int {
        return AAttributeInputView.VIEW_TYPE_SHORT_TEXT
    }

}