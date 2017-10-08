package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.TimeRangePicker
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.TimeRangePickerInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTTimeSpanAttributeHelper : OTAttributeHelper() {

    companion object {
        const val PROPERTY_GRANULARITY = "granularity"
        const val PROPERTY_TYPE = "type"

        const val GRANULARITY_DAY = 0
        const val GRANULARITY_MINUTE = 1
    }

    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics = NumericCharacteristics(true, false)

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int {
        return R.string.type_timespan_name
    }

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int {
        return R.drawable.icon_small_timer
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_TIMESPAN

    override val propertyKeys: Array<String> = arrayOf(PROPERTY_GRANULARITY)

    override fun <T> parsePropertyValue(propertyKey: String, serializedValue: String): T? {
        return when (propertyKey) {
        //PROPERTY_TYPE->OTPropertyManager.getHelper(OTPropertyManager.EPropertyType.Selection).parseValue(serializedValue)
            PROPERTY_GRANULARITY -> OTPropertyManager.getHelper(OTPropertyManager.EPropertyType.Selection).parseValue(serializedValue)
            else -> throw IllegalArgumentException("Unsupported property type ${propertyKey}")
        } as T
    }

    fun getGranularity(attribute: OTAttributeDAO): Int {
        return getDeserializedPropertyValue<Int>(PROPERTY_GRANULARITY, attribute) ?: GRANULARITY_MINUTE
    }

    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int = AAttributeInputView.VIEW_TYPE_TIME_RANGE_PICKER

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>, attribute: OTAttributeDAO) {
        if (inputView is TimeRangePickerInputView) {
            val granularity = when (getGranularity(attribute)) {
                GRANULARITY_DAY -> TimeRangePicker.Granularity.DATE
                GRANULARITY_MINUTE -> TimeRangePicker.Granularity.TIME
                else -> TimeRangePicker.Granularity.TIME
            }

            inputView.setGranularity(granularity)
        }
    }
}