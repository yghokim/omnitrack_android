package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.content.Context
import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.FallbackPolicyResolver
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.AFieldValueSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.TimePointSorter
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
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTTimeAttributeHelper : OTAttributeHelper() {

    companion object {
        const val GRANULARITY = "granularity"

        const val GRANULARITY_DAY = 0
        const val GRANULARITY_MINUTE = 1
        const val GRANULARITY_SECOND = 2

        val formats = mapOf<Int, SimpleDateFormat>(
                Pair(GRANULARITY_DAY, SimpleDateFormat(OTApp.getString(R.string.property_time_format_granularity_day))),
                Pair(GRANULARITY_MINUTE, SimpleDateFormat(OTApp.getString(R.string.property_time_format_granularity_minute))),
                Pair(GRANULARITY_SECOND, SimpleDateFormat(OTApp.getString(R.string.property_time_format_granularity_second)))
        )

        private val timezoneSizeSpan = AbsoluteSizeSpan(OTApp.instance.resourcesWrapped.getDimensionPixelSize(R.dimen.tracker_list_element_information_text_headerSize))
        private val timezoneStyleSpan = StyleSpan(Typeface.BOLD)
        private val timezoneColorSpan = ForegroundColorSpan(ContextCompat.getColor(OTApp.instance.contextCompat, R.color.textColorLight))

    }

    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics = NumericCharacteristics(true, true)

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int {
        return R.string.type_timepoint_name
    }

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int {
        return R.drawable.icon_small_time
    }

    override val supportedFallbackPolicies: LinkedHashMap<Int, FallbackPolicyResolver>
        get() = super.supportedFallbackPolicies.apply {
            this[OTAttributeDAO.DEFAULT_VALUE_POLICY_FILL_WITH_INTRINSIC_VALUE] = object : FallbackPolicyResolver(R.string.msg_intrinsic_time, isValueVolatile = true) {
                override fun getFallbackValue(attribute: OTAttributeDAO, realm: Realm): Single<Nullable<out Any>> {
                    return Single.just(Nullable(TimePoint()))
                }

            }

            this.remove(OTAttributeDAO.DEFAULT_VALUE_POLICY_NULL)
        }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_TIMEPOINT

    override fun getSupportedSorters(attribute: OTAttributeDAO): Array<AFieldValueSorter> {
        return arrayOf(TimePointSorter(attribute.name, attribute.localId))
    }

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
            GRANULARITY -> OTApp.getString(R.string.property_time_granularity)
            else -> ""
        }
    }

    override fun makePropertyView(propertyKey: String, context: Context): APropertyView<out Any> {
        val superView = super.makePropertyView(propertyKey, context)
        if (propertyKey == GRANULARITY && superView is SelectionPropertyView) {
            superView.setEntries(arrayOf(OTApp.instance.resourcesWrapped.getString(R.string.property_time_granularity_day),
                    OTApp.instance.resourcesWrapped.getString(R.string.property_time_granularity_minute),
                    OTApp.instance.resourcesWrapped.getString(R.string.property_time_granularity_second)

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

    override fun initialize(attribute: OTAttributeDAO) {
        attribute.fallbackValuePolicy = OTAttributeDAO.DEFAULT_VALUE_POLICY_FILL_WITH_INTRINSIC_VALUE
    }

    override fun formatAttributeValue(attribute: OTAttributeDAO, value: Any): CharSequence {
        return if (value is TimePoint) {
            val granularity = getGranularity(attribute)
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = value.timestamp
            calendar.timeZone = value.timeZone

            calendar.set(Calendar.MILLISECOND, 0)

            if (granularity == GRANULARITY_DAY) {
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
            }

            val timeString = formats[granularity]!!.format(calendar.time)
            val timeZoneName = value.timeZone.displayName
            val start = timeString.length + 1
            val end = timeString.length + 1 + timeZoneName.length

            return SpannableString("$timeString\n$timeZoneName").apply {
                setSpan(timezoneSizeSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(timezoneStyleSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(timezoneColorSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } else return ""
    }
}