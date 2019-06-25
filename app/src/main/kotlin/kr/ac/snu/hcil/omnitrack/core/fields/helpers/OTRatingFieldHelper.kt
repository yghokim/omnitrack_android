package kr.ac.snu.hcil.omnitrack.core.fields.helpers

import android.content.Context
import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.fields.FallbackPolicyResolver
import kr.ac.snu.hcil.omnitrack.core.fields.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.core.fields.logics.AFieldValueSorter
import kr.ac.snu.hcil.omnitrack.core.fields.logics.NumericSorter
import kr.ac.snu.hcil.omnitrack.core.fields.properties.OTPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.fields.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.core.types.Fraction
import kr.ac.snu.hcil.omnitrack.core.types.RatingOptions

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTRatingFieldHelper(context: Context) : OTFieldHelper(context), ISingleNumberFieldHelper {

    companion object {
        const val PROPERTY_OPTIONS = "options"
    }

    inner class MiddleValueFallbackPolicyResolver : FallbackPolicyResolver(context.applicationContext, R.string.msg_intrinsic_rating, false) {
        override fun getFallbackValue(field: OTFieldDAO, realm: Realm): Single<Nullable<out Any>> {
            return Single.defer {
                val ratingOptions = getRatingOptions(field)
                return@defer Single.just(Nullable(Fraction.fromRatioAndUnder(0.5f, ratingOptions.getMaximumPrecisionIntegerRangeLength())))
            }
        }

    }

    override fun getValueNumericCharacteristics(field: OTFieldDAO): NumericCharacteristics = NumericCharacteristics(true, true)

    override fun getTypeNameResourceId(field: OTFieldDAO): Int {
        return R.string.type_rating_name
    }

    override fun getTypeSmallIconResourceId(field: OTFieldDAO): Int {
        return R.drawable.icon_small_star //TODO Options
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_FRACTION

    override fun getSupportedSorters(field: OTFieldDAO): Array<AFieldValueSorter> {
        return arrayOf(NumericSorter(field.name, field.localId))
    }

    override val propertyKeys: Array<String> = arrayOf(PROPERTY_OPTIONS)

    override fun <T> getPropertyHelper(propertyKey: String): OTPropertyHelper<T> {
        return when (propertyKey) {
            PROPERTY_OPTIONS -> propertyManager.getHelper(OTPropertyManager.EPropertyType.RatingOptions)
            else -> throw IllegalArgumentException("Unsupported property type: $propertyKey")
        } as OTPropertyHelper<T>
    }

    fun getRatingOptions(field: OTFieldDAO): RatingOptions {
        return getDeserializedPropertyValue<RatingOptions>(PROPERTY_OPTIONS, field)
                ?: RatingOptions(context.applicationContext)
    }

    override fun getPropertyInitialValue(propertyKey: String): Any? {
        return when (propertyKey) {
            PROPERTY_OPTIONS -> RatingOptions(context.applicationContext)
            else -> null
        }
    }


    override val supportedFallbackPolicies: LinkedHashMap<String, FallbackPolicyResolver>
        get() = super.supportedFallbackPolicies.apply{
            this[OTFieldDAO.DEFAULT_VALUE_POLICY_FILL_WITH_INTRINSIC_VALUE] = MiddleValueFallbackPolicyResolver()
        }


    override fun formatAttributeValue(field: OTFieldDAO, value: Any): CharSequence {
        val ratingOptions = getRatingOptions(field)
        return when (ratingOptions.type) {
            RatingOptions.DisplayType.Star -> "${ratingOptions.convertFractionToRealScore(value as Fraction)} / ${ratingOptions.stars}"
            RatingOptions.DisplayType.Likert -> ratingOptions.convertFractionToRealScore(value as Fraction).toString()
        }
    }


    override fun convertValueToSingleNumber(value: Any, field: OTFieldDAO): Double {
        if (value is Number) return value.toDouble()
        else {
            val ratingOptions = getRatingOptions(field)
            return ratingOptions.convertFractionToRealScore(value as Fraction).toDouble()
        }
    }
}