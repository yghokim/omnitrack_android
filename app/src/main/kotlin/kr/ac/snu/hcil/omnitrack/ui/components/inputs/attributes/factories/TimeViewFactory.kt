package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.factories

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTTimeAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.DateTimePicker
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AttributeViewFactory
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.TimePointInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.SelectionPropertyView

class TimeViewFactory(helper: OTTimeAttributeHelper) : AttributeViewFactory<OTTimeAttributeHelper>(helper) {
    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int {
        return AAttributeInputView.VIEW_TYPE_TIME_POINT
    }

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>, attribute: OTAttributeDAO) {
        if (inputView is TimePointInputView) {
            when (helper.getGranularity(attribute)) {
                OTTimeAttributeHelper.GRANULARITY_DAY -> inputView.setPickerMode(DateTimePicker.DATE)
                OTTimeAttributeHelper.GRANULARITY_MINUTE -> inputView.setPickerMode(DateTimePicker.MINUTE)
                OTTimeAttributeHelper.GRANULARITY_SECOND -> inputView.setPickerMode(DateTimePicker.SECOND)
            }
        }
    }


    override fun makePropertyView(propertyKey: String, context: Context): APropertyView<out Any> {
        val superView = super.makePropertyView(propertyKey, context)
        if (propertyKey == OTTimeAttributeHelper.GRANULARITY && superView is SelectionPropertyView) {
            superView.setEntries(arrayOf(context.resources.getString(R.string.property_time_granularity_day),
                    context.resources.getString(R.string.property_time_granularity_minute),
                    context.resources.getString(R.string.property_time_granularity_second)

            ))
        }

        return superView
    }
}