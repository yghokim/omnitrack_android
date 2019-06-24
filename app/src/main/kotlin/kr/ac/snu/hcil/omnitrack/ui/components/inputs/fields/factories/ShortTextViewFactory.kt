package kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.factories

import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTShortTextFieldHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.OTFieldViewFactory

class ShortTextViewFactory(helper: OTShortTextFieldHelper) : OTFieldViewFactory<OTShortTextFieldHelper>(helper) {
    override fun getInputViewType(previewMode: Boolean, field: OTFieldDAO): Int {
        return AFieldInputView.VIEW_TYPE_SHORT_TEXT
    }

}