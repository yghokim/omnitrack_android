package kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.factories

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTTimeSpanFieldHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.OTFieldViewFactory
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.TimeRangePickerInputView
import kr.ac.snu.hcil.omnitrack.views.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.views.properties.SelectionPropertyView
import kr.ac.snu.hcil.omnitrack.views.time.TimeRangePicker

class TimeSpanViewFactory(helper: OTTimeSpanFieldHelper) : OTFieldViewFactory<OTTimeSpanFieldHelper>(helper) {
    override fun getInputViewType(previewMode: Boolean, field: OTFieldDAO): Int {
        return AFieldInputView.VIEW_TYPE_TIME_RANGE_PICKER
    }

    override fun refreshInputViewUI(inputView: AFieldInputView<out Any>, field: OTFieldDAO) {
        if (inputView is TimeRangePickerInputView) {
            val granularity = when (helper.getGranularity(field)) {
                OTTimeSpanFieldHelper.GRANULARITY_DAY -> TimeRangePicker.Granularity.DATE
                OTTimeSpanFieldHelper.GRANULARITY_MINUTE -> TimeRangePicker.Granularity.TIME
                else -> TimeRangePicker.Granularity.TIME
            }

            inputView.setGranularity(granularity)
        }
    }


    override fun makePropertyView(propertyKey: String, context: Context): APropertyView<out Any> {
        val superView = super.makePropertyView(propertyKey, context)
        if (propertyKey == OTTimeSpanFieldHelper.PROPERTY_GRANULARITY && superView is SelectionPropertyView) {
            superView.setEntries(arrayOf(context.resources.getString(R.string.property_time_granularity_day),
                    context.resources.getString(R.string.property_time_granularity_minute)
            ))
        }

        return superView
    }
}