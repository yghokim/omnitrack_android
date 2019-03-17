package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.factories

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTTimeSpanAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AttributeViewFactory
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.TimeRangePickerInputView
import kr.ac.snu.hcil.omnitrack.views.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.views.properties.SelectionPropertyView
import kr.ac.snu.hcil.omnitrack.views.time.TimeRangePicker

class TimeSpanViewFactory(helper: OTTimeSpanAttributeHelper) : AttributeViewFactory<OTTimeSpanAttributeHelper>(helper) {
    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int {
        return AAttributeInputView.VIEW_TYPE_TIME_RANGE_PICKER
    }

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>, attribute: OTAttributeDAO) {
        if (inputView is TimeRangePickerInputView) {
            val granularity = when (helper.getGranularity(attribute)) {
                OTTimeSpanAttributeHelper.GRANULARITY_DAY -> TimeRangePicker.Granularity.DATE
                OTTimeSpanAttributeHelper.GRANULARITY_MINUTE -> TimeRangePicker.Granularity.TIME
                else -> TimeRangePicker.Granularity.TIME
            }

            inputView.setGranularity(granularity)
        }
    }


    override fun makePropertyView(propertyKey: String, context: Context): APropertyView<out Any> {
        val superView = super.makePropertyView(propertyKey, context)
        if (propertyKey == OTTimeSpanAttributeHelper.PROPERTY_GRANULARITY && superView is SelectionPropertyView) {
            superView.setEntries(arrayOf(context.resources.getString(R.string.property_time_granularity_day),
                    context.resources.getString(R.string.property_time_granularity_minute)
            ))
        }

        return superView
    }
}