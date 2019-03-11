package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.content.ContextCompat
import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.FallbackPolicyResolver
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.AFieldValueSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.TimePointSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.core.types.TimePoint
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTTimeAttributeHelper(context: Context) : OTAttributeHelper(context) {

    companion object {
        const val GRANULARITY = "granularity"

        const val GRANULARITY_DAY = 0
        const val GRANULARITY_MINUTE = 1
        const val GRANULARITY_SECOND = 2


    }


    val formats = mapOf(
            Pair(GRANULARITY_DAY, SimpleDateFormat(context.getString(R.string.property_time_format_granularity_day))),
            Pair(GRANULARITY_MINUTE, SimpleDateFormat(context.getString(R.string.property_time_format_granularity_minute))),
            Pair(GRANULARITY_SECOND, SimpleDateFormat(context.getString(R.string.property_time_format_granularity_second)))
    )

    private val app = context.applicationContext as OTAndroidApp

    private val timezoneSizeSpan = AbsoluteSizeSpan(context.resources.getDimensionPixelSize(R.dimen.tracker_list_element_information_text_headerSize))
    private val timezoneStyleSpan = StyleSpan(Typeface.BOLD)
    private val timezoneColorSpan = ForegroundColorSpan(ContextCompat.getColor(context, R.color.textColorLight))


    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics = NumericCharacteristics(true, true)

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int {
        return R.string.type_timepoint_name
    }

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int {
        return R.drawable.icon_small_time
    }

    override val supportedFallbackPolicies: LinkedHashMap<Int, FallbackPolicyResolver>
        get() = super.supportedFallbackPolicies.apply {
            this[OTAttributeDAO.DEFAULT_VALUE_POLICY_FILL_WITH_INTRINSIC_VALUE] = object : FallbackPolicyResolver(context.applicationContext, R.string.msg_intrinsic_time, isValueVolatile = true) {
                override fun getFallbackValue(attribute: OTAttributeDAO, realm: Realm): Single<Nullable<out Any>> {
                    return Single.just(Nullable(TimePoint(System.currentTimeMillis(), app.applicationComponent.getPreferredTimeZone().id)))
                }

            }

            this.remove(OTAttributeDAO.DEFAULT_VALUE_POLICY_NULL)
        }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_TIMEPOINT

    override fun getSupportedSorters(attribute: OTAttributeDAO): Array<AFieldValueSorter> {
        return arrayOf(TimePointSorter(attribute.name, attribute.localId))
    }

    override val propertyKeys: Array<String> = arrayOf(GRANULARITY)

    override fun <T> getPropertyHelper(propertyKey: String): OTPropertyHelper<T> {
        return when (propertyKey) {
            GRANULARITY -> propertyManager.getHelper(OTPropertyManager.EPropertyType.Selection)
            else -> throw IllegalArgumentException("Unsupported property type $propertyKey")
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
            GRANULARITY -> context.applicationContext.getString(R.string.property_time_granularity)
            else -> ""
        }
    }


    fun getGranularity(attribute: OTAttributeDAO): Int {
        return getDeserializedPropertyValue<Int>(GRANULARITY, attribute) ?: GRANULARITY_SECOND
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