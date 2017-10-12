package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.content.Context
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.TimeRangePicker
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.TimeRangePickerInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.SelectionPropertyView
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

    override fun isIntrinsicDefaultValueVolatile(attribute: OTAttributeDAO): Boolean = true

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_TIMESPAN

    override val propertyKeys: Array<String> = arrayOf(PROPERTY_GRANULARITY)

    override fun <T> getPropertyHelper(propertyKey: String): OTPropertyHelper<T> {
        return when (propertyKey) {
        //PROPERTY_TYPE->OTPropertyManager.getHelper(OTPropertyManager.EPropertyType.Selection).parseValue(serializedValue)
            PROPERTY_GRANULARITY -> OTPropertyManager.getHelper(OTPropertyManager.EPropertyType.Selection)
            else -> throw IllegalArgumentException("Unsupported property type ${propertyKey}")
        } as OTPropertyHelper<T>
    }

    override fun makePropertyView(propertyKey: String, context: Context): APropertyView<out Any> {
        val superView = super.makePropertyView(propertyKey, context)
        if (propertyKey == PROPERTY_GRANULARITY && superView is SelectionPropertyView) {
            superView.setEntries(arrayOf(OTApplication.app.resourcesWrapped.getString(R.string.property_time_granularity_day),
                    OTApplication.app.resourcesWrapped.getString(R.string.property_time_granularity_minute)
            ))
        }

        return superView
    }

    fun getGranularity(attribute: OTAttributeDAO): Int {
        return getDeserializedPropertyValue<Int>(PROPERTY_GRANULARITY, attribute) ?: GRANULARITY_MINUTE
    }

    override fun getPropertyTitle(propertyKey: String): String {
        return when (propertyKey) {
            PROPERTY_GRANULARITY -> OTApplication.getString(R.string.property_time_granularity)
            else -> ""
        }
    }

    override fun getPropertyInitialValue(propertyKey: String): Any? {
        return when (propertyKey) {
            PROPERTY_GRANULARITY -> GRANULARITY_DAY
            else -> null
        }
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

    override fun initialize(attribute: OTAttributeDAO) {
        attribute.fallbackValuePolicy = OTAttributeDAO.DEFAULT_VALUE_POLICY_FILL_WITH_INTRINSIC_VALUE
    }
}