package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.content.Context
import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.FallbackPolicyResolver
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.AFieldValueSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.NumericSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.core.types.Fraction
import kr.ac.snu.hcil.omnitrack.core.types.RatingOptions
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTRatingAttributeHelper(context: Context) : OTAttributeHelper(context), ISingleNumberAttributeHelper {

    companion object {
        const val PROPERTY_OPTIONS = "options"
    }

    inner class MiddleValueFallbackPolicyResolver : FallbackPolicyResolver(context.applicationContext, R.string.msg_intrinsic_rating, false) {
        override fun getFallbackValue(attribute: OTAttributeDAO, realm: Realm): Single<Nullable<out Any>> {
            return Single.defer {
                val ratingOptions = getRatingOptions(attribute)
                return@defer Single.just(Nullable(Fraction.fromRatioAndUnder(0.5f, ratingOptions.getMaximumPrecisionIntegerRangeLength())))
            }
        }

    }

    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics = NumericCharacteristics(true, true)

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int {
        return R.string.type_rating_name
    }

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int {
        return R.drawable.icon_small_star //TODO Options
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_FRACTION

    override fun getSupportedSorters(attribute: OTAttributeDAO): Array<AFieldValueSorter> {
        return arrayOf(NumericSorter(attribute.name, attribute.localId))
    }

    override val propertyKeys: Array<String> = arrayOf(PROPERTY_OPTIONS)

    override fun <T> getPropertyHelper(propertyKey: String): OTPropertyHelper<T> {
        return when (propertyKey) {
            PROPERTY_OPTIONS -> propertyManager.getHelper(OTPropertyManager.EPropertyType.RatingOptions)
            else -> throw IllegalArgumentException("Unsupported property type: $propertyKey")
        } as OTPropertyHelper<T>
    }

    fun getRatingOptions(attribute: OTAttributeDAO): RatingOptions {
        return getDeserializedPropertyValue<RatingOptions>(PROPERTY_OPTIONS, attribute)
                ?: RatingOptions(context.applicationContext)
    }

    override fun getPropertyInitialValue(propertyKey: String): Any? {
        return when (propertyKey) {
            PROPERTY_OPTIONS -> RatingOptions(context.applicationContext)
            else -> null
        }
    }




    override val supportedFallbackPolicies: LinkedHashMap<Int, FallbackPolicyResolver>
        get() = super.supportedFallbackPolicies.apply{
            this[OTAttributeDAO.DEFAULT_VALUE_POLICY_FILL_WITH_INTRINSIC_VALUE] = MiddleValueFallbackPolicyResolver()
        }


    override fun formatAttributeValue(attribute: OTAttributeDAO, value: Any): CharSequence {
        val ratingOptions = getRatingOptions(attribute)
        return when (ratingOptions.type) {
            RatingOptions.DisplayType.Star -> "${ratingOptions.convertFractionToRealScore(value as Fraction)} / ${ratingOptions.stars}"
            RatingOptions.DisplayType.Likert -> ratingOptions.convertFractionToRealScore(value as Fraction).toString()
        }
    }


    override fun convertValueToSingleNumber(value: Any, attribute: OTAttributeDAO): Double {
        if (value is Number) return value.toDouble()
        else {
            val ratingOptions = getRatingOptions(attribute)
            return ratingOptions.convertFractionToRealScore(value as Fraction).toDouble()
        }
    }
}