package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.content.Context
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.FallbackPolicyResolver
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.AFieldValueSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.TimeSpanIntervalSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.TimeSpanPivotalSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTItemValueEntryDAO
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.TimeRangePicker
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.TimeRangePickerInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.SelectionPropertyView
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTTimeSpanAttributeHelper : OTAttributeHelper() {

    companion object {
        const val PROPERTY_GRANULARITY = "granularity"
        const val PROPERTY_TYPE = "type"

        const val FALLBACK_POLICY_ID_TIMESPAN_CONNECT_PREVIOUS = 11

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

    override fun getSupportedSorters(attribute: OTAttributeDAO): Array<AFieldValueSorter> {
        return arrayOf(
                TimeSpanPivotalSorter("${attribute.name} start", true, attribute.localId),
                TimeSpanPivotalSorter("${attribute.name} end", false, attribute.localId),
                TimeSpanIntervalSorter("${attribute.name} interval", attribute.localId)
        )
    }

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
            superView.setEntries(arrayOf(OTApp.instance.resourcesWrapped.getString(R.string.property_time_granularity_day),
                    OTApp.instance.resourcesWrapped.getString(R.string.property_time_granularity_minute)
            ))
        }

        return superView
    }

    fun getGranularity(attribute: OTAttributeDAO): Int {
        return getDeserializedPropertyValue<Int>(PROPERTY_GRANULARITY, attribute) ?: GRANULARITY_MINUTE
    }

    override fun getPropertyTitle(propertyKey: String): String {
        return when (propertyKey) {
            PROPERTY_GRANULARITY -> OTApp.getString(R.string.property_time_granularity)
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

    override val supportedFallbackPolicies: LinkedHashMap<Int, FallbackPolicyResolver>
        get() = super.supportedFallbackPolicies.apply {
            this[OTAttributeDAO.DEFAULT_VALUE_POLICY_FILL_WITH_INTRINSIC_VALUE] = object : FallbackPolicyResolver(R.string.msg_intrinsic_time, isValueVolatile = true) {
                override fun getFallbackValue(attribute: OTAttributeDAO, realm: Realm): Single<Nullable<out Any>> {
                    return Single.just(Nullable(TimeSpan()))
                }
            }

            this[FALLBACK_POLICY_ID_TIMESPAN_CONNECT_PREVIOUS] = object : FallbackPolicyResolver(R.string.msg_attribute_fallback_policy_timespan_connect_previous, isValueVolatile = true) {
                override fun getFallbackValue(attribute: OTAttributeDAO, realm: Realm): Single<Nullable<out Any>> {
                    return Single.defer {
                        val previousNotNullEntry = try {
                            realm.where(OTItemValueEntryDAO::class.java)
                                    .equalTo("key", attribute.localId)
                                    .equalTo("items.trackerId", attribute.trackerId)
                                    .equalTo("items.removed", false)
                                    .isNotNull("value")
                                    .beginsWith("value", "${TypeStringSerializationHelper.TYPENAME_TIMESPAN.length}${TypeStringSerializationHelper.TYPENAME_TIMESPAN}")
                                    .findAll().sortedByDescending { (it.items?.firstOrNull()?.getValueOf(attribute.localId) as TimeSpan).to }.firstOrNull()
                        } catch (ex: NoSuchElementException) {
                            null
                        }

                        val previousTimespan = previousNotNullEntry?.value?.let { TypeStringSerializationHelper.deserialize(it) as? TimeSpan }
                        return@defer if (previousTimespan != null) {
                            Single.just<Nullable<out Any>>(
                                    Nullable(TimeSpan.fromPoints(previousTimespan.to, System.currentTimeMillis())))
                        } else Single.just<Nullable<out Any>>(Nullable(null))
                    }.subscribeOn(AndroidSchedulers.mainThread())
                }
            }

            this.remove(OTAttributeDAO.DEFAULT_VALUE_POLICY_NULL)
        }

    override fun initialize(attribute: OTAttributeDAO) {
        attribute.fallbackValuePolicy = OTAttributeDAO.DEFAULT_VALUE_POLICY_FILL_WITH_INTRINSIC_VALUE
    }

    override fun formatAttributeValue(attribute: OTAttributeDAO, value: Any): CharSequence {
        return (value as? TimeSpan)?.let {
            val granularity = getGranularity(attribute)
            val format = when (granularity) {
                GRANULARITY_DAY -> OTTimeAttributeHelper.formats[GRANULARITY_DAY]!!
                GRANULARITY_MINUTE -> OTTimeAttributeHelper.formats[GRANULARITY_MINUTE]!!
                else -> OTTimeAttributeHelper.formats[GRANULARITY_MINUTE]!!
            }

            val from = format.format(Date(it.from))
            val to = format.format(Date(it.to))

            val overlapUntil = 0
            /*
            while( from[overlapUntil] == to[overlapUntil] )
            {
                overlapUntil++
                if(overlapUntil>=from.length || overlapUntil >= to.length){break;}
            }*/

            val builder = StringBuilder(from)
            builder.append(" ~ ").append(to.subSequence(overlapUntil, to.length)).toString()

        } ?: ""
    }

    /*
    override fun makeRecommendedChartModels(attribute: OTAttributeDAO, realm: Realm): Array<ChartModel<*>> {
        return arrayOf(DurationTimelineModel(attribute, realm))
    }*/

    override fun onAddColumnToTable(attribute: OTAttributeDAO, out: MutableList<String>) {
        out.add("${getAttributeUniqueName(attribute)}_start_epoch")
        out.add("${getAttributeUniqueName(attribute)}_end_epoch")
        out.add("${getAttributeUniqueName(attribute)}_duration_millis")
        out.add("${getAttributeUniqueName(attribute)}_timezone")
    }

    override fun onAddValueToTable(attribute: OTAttributeDAO, value: Any?, out: MutableList<String?>, uniqKey: String?) {
        if (value is TimeSpan) {

            val formatter = (TimeHelper.FORMAT_ISO_8601.clone() as SimpleDateFormat).apply { timeZone = value.timeZone }
            val fromFormatted = formatter.format(Date(value.from))
            val toFormatted = formatter.format(Date(value.to))
            out.add(fromFormatted)
            out.add(toFormatted)
            out.add(value.duration.toString())
            out.add(value.timeZone.getDisplayName(Locale.ENGLISH))

        } else {
            out.add(null)
            out.add(null)
            out.add(null)
            out.add(null)
        }
    }
}