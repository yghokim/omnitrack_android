package kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.factories

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTTimeFieldHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.OTFieldViewFactory
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.TimePointInputView
import kr.ac.snu.hcil.omnitrack.views.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.views.properties.SelectionPropertyView
import kr.ac.snu.hcil.omnitrack.views.time.DateTimePicker

class TimeViewFactory(helper: OTTimeFieldHelper) : OTFieldViewFactory<OTTimeFieldHelper>(helper) {
    override fun getInputViewType(previewMode: Boolean, field: OTFieldDAO): Int {
        return AFieldInputView.VIEW_TYPE_TIME_POINT
    }

    override fun refreshInputViewUI(inputView: AFieldInputView<out Any>, field: OTFieldDAO) {
        if (inputView is TimePointInputView) {
            when (helper.getGranularity(field)) {
                OTTimeFieldHelper.GRANULARITY_DAY -> inputView.setPickerMode(DateTimePicker.DATE)
                OTTimeFieldHelper.GRANULARITY_MINUTE -> inputView.setPickerMode(DateTimePicker.MINUTE)
                OTTimeFieldHelper.GRANULARITY_SECOND -> inputView.setPickerMode(DateTimePicker.SECOND)
            }
        }
    }


    override fun makePropertyView(propertyKey: String, context: Context): APropertyView<out Any> {
        val superView = super.makePropertyView(propertyKey, context)
        if (propertyKey == OTTimeFieldHelper.GRANULARITY && superView is SelectionPropertyView) {
            superView.setEntries(arrayOf(context.resources.getString(R.string.property_time_granularity_day),
                    context.resources.getString(R.string.property_time_granularity_minute),
                    context.resources.getString(R.string.property_time_granularity_second)

            ))
        }

        return superView
    }
}