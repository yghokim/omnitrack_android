package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.content.Context
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.DateTimePicker
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.TimePointInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.SelectionPropertyView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable

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

    override fun <T> getPropertyHelper(propertyKey: String): OTPropertyHelper<T> {
        return when (propertyKey) {
            GRANULARITY -> OTPropertyManager.getHelper(OTPropertyManager.EPropertyType.Selection)
            else -> throw IllegalArgumentException("Unsupported property type ${propertyKey}")
        } as OTPropertyHelper<T>
    }

    override fun getPropertyInitialValue(propertyKey: String): Any? {
        return when (propertyKey) {
            GRANULARITY -> GRANULARITY_DAY
            else -> null
        }
    }

    override fun getPropertyTitle(propertyKey: String): String {
        return when (propertyKey) {
            GRANULARITY -> OTApplication.getString(R.string.property_time_granularity)
            else -> ""
        }
    }

    override fun makePropertyView(propertyKey: String, context: Context): APropertyView<out Any> {
        val superView = super.makePropertyView(propertyKey, context)
        if (propertyKey == GRANULARITY && superView is SelectionPropertyView) {
            superView.setEntries(arrayOf(OTApplication.app.resourcesWrapped.getString(R.string.property_time_granularity_day),
                    OTApplication.app.resourcesWrapped.getString(R.string.property_time_granularity_minute),
                    OTApplication.app.resourcesWrapped.getString(R.string.property_time_granularity_second)

            ))
        }

        return superView
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

    override fun isIntrinsicDefaultValueSupported(attribute: OTAttributeDAO): Boolean {
        return true
    }

    override fun makeIntrinsicDefaultValue(attribute: OTAttributeDAO): Observable<out Any> {
        return Observable.defer { Observable.just(TimePoint()) }
    }

    override fun makeIntrinsicDefaultValueMessage(attribute: OTAttributeDAO): CharSequence {
        return OTApplication.getString(R.string.msg_intrinsic_time)
    }
}