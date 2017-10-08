package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.DateTimePicker
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.TimePointInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTTimeAttributeHelper : OTAttributeHelper() {

    companion object {
        const val GRANULARITY = "granularity"

        const val GRANULARITY_DAY = 0
        const val GRANULARITY_MINUTE = 1
        const val GRANULARITY_SECOND = 2
    }

    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics = NumericCharacteristics(true, true)

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int {
        return R.string.type_timepoint_name
    }

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int {
        return R.drawable.icon_small_time
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_TIMEPOINT

    override val propertyKeys: Array<String> = arrayOf(GRANULARITY)

    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int = AAttributeInputView.VIEW_TYPE_TIME_POINT

    override fun <T> parsePropertyValue(propertyKey: String, serializedValue: String): T? {
        return when (propertyKey) {
            GRANULARITY -> OTPropertyManager.getHelper(OTPropertyManager.EPropertyType.Selection).parseValue(serializedValue)
            else -> throw IllegalArgumentException("Unsupported property type ${propertyKey}")
        } as T
    }

    fun getGranularity(attribute: OTAttributeDAO): Int {
        return getDeserializedPropertyValue<Int>(GRANULARITY, attribute) ?: GRANULARITY_SECOND
    }

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>, attribute: OTAttributeDAO) {
        if (inputView is TimePointInputView) {
            when (getGranularity(attribute)) {
                GRANULARITY_DAY -> inputView.setPickerMode(DateTimePicker.DATE)
                GRANULARITY_MINUTE -> inputView.setPickerMode(DateTimePicker.MINUTE)
                GRANULARITY_SECOND -> inputView.setPickerMode(DateTimePicker.SECOND)
            }

            inputView.value = TimePoint()

        }
    }

}