package kr.ac.snu.hcil.omnitrack.core.attributes

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTSelectionProperty
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.ui.components.TimeRangePicker
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.TimeRangePickerInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by younghokim on 16. 8. 6..
 */
class OTTimeSpanAttribute(objectId: String?, dbId: Long?, columnName: String, settingData: String?) : OTAttribute<TimeSpan>(objectId, dbId, columnName, TYPE_TIMESPAN, settingData) {

    companion object {
        const val PROPERTY_GRANULARITY = 0
        const val PROPERTY_TYPE = 1
    }

    override val propertyKeys: Array<Int> = arrayOf(PROPERTY_GRANULARITY, PROPERTY_TYPE)

    override val typeNameResourceId: Int = R.string.type_timespan_name

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_TIMESPAN

    override fun createProperties() {
        //TODO I18n
        assignProperty(OTSelectionProperty(PROPERTY_GRANULARITY, "Granularity", arrayOf("Date", "Time")))
        assignProperty(OTSelectionProperty(PROPERTY_TYPE, "Interface Type", arrayOf("Range Picker", "Stopwatch")))

        setPropertyValue(PROPERTY_GRANULARITY, 0)
    }


    override fun formatAttributeValue(value: Any): String {
        return ""
    }

    override fun getAutoCompleteValueAsync(resultHandler: (TimeSpan) -> Unit) {
        resultHandler.invoke(TimeSpan())
    }

    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_TIME_RANGE_PICKER
    }

    override fun refreshInputViewContents(inputView: AAttributeInputView<out Any>) {
        if(inputView is TimeRangePickerInputView)
        {
            val granularity = when(getPropertyValue<Int>(PROPERTY_GRANULARITY))
            {
                0->TimeRangePicker.Granularity.DATE
                1->TimeRangePicker.Granularity.TIME
                else->TimeRangePicker.Granularity.TIME
            }

            inputView.setGranularity(granularity)
        }
    }


}