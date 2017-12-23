package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.content.Context
import android.view.View
import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.FallbackPolicyResolver
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.AFieldValueSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.NumericSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.datatypes.Fraction
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.common.StarRatingSlider
import kr.ac.snu.hcil.omnitrack.ui.components.common.StarScoreView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.LikertScaleInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.StarRatingInputView
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.RatingOptions
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTRatingAttributeHelper(configuredContext: ConfiguredContext) : OTAttributeHelper(configuredContext), ISingleNumberAttributeHelper {

    companion object {
        const val PROPERTY_OPTIONS = "options"
    }

    inner class MiddleValueFallbackPolicyResolver : FallbackPolicyResolver(R.string.msg_intrinsic_rating, false) {
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
            PROPERTY_OPTIONS -> OTPropertyManager.getHelper(OTPropertyManager.EPropertyType.RatingOptions)
            else -> throw IllegalArgumentException("Unsupported property type: ${propertyKey}")
        } as OTPropertyHelper<T>
    }

    fun getRatingOptions(attribute: OTAttributeDAO): RatingOptions {
        return getDeserializedPropertyValue<RatingOptions>(PROPERTY_OPTIONS, attribute) ?: RatingOptions()
    }

    override fun getPropertyInitialValue(propertyKey: String): Any? {
        return when (propertyKey) {
            PROPERTY_OPTIONS -> RatingOptions()
            else -> null
        }
    }


    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int {
        return when (getRatingOptions(attribute).type) {
            RatingOptions.DisplayType.Star -> AAttributeInputView.VIEW_TYPE_RATING_STARS
            RatingOptions.DisplayType.Likert -> AAttributeInputView.VIEW_TYPE_RATING_LIKERT
        }
    }

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>, attribute: OTAttributeDAO) {
        val options = getRatingOptions(attribute)
        if (inputView is StarRatingInputView) {
            inputView.ratingView.isFractional = options.isFractional
            inputView.ratingView.levels = options.stars
            //inputView.ratingView.score = options.stars.maxScore / 2.0f
        } else if (inputView is LikertScaleInputView) {
            inputView.scalePicker.isFractional = options.isFractional
            inputView.scalePicker.leftMost = options.leftMost
            inputView.scalePicker.rightMost = options.rightMost
            inputView.scalePicker.leftLabel = options.leftLabel
            inputView.scalePicker.rightLabel = options.rightLabel
            inputView.scalePicker.middleLabel = options.middleLabel
            //inputView.scalePicker.value = ((options.rightMost + options.leftMost) shr 1).toFloat()
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

    //item list=========================

    override fun getViewForItemList(attribute: OTAttributeDAO, context: Context, recycledView: View?): View {
        val ratingOptions = getRatingOptions(attribute)

        when (ratingOptions.type) {
            RatingOptions.DisplayType.Star -> {

                if (ratingOptions.stars <= 5) {
                    val target = recycledView as? StarRatingSlider ?: StarRatingSlider(context)

                    target.isLightMode = true
                    target.overridenIntrinsicWidth = context.resources.getDimensionPixelSize(R.dimen.star_rating_item_list_view_unit_size)
                    target.overridenIntrinsicHeight = target.overridenIntrinsicWidth
                    return target
                } else {
                    val target = recycledView as? StarScoreView ?: StarScoreView(context)
                    return target
                }
            }

            RatingOptions.DisplayType.Likert -> {
                return super.getViewForItemList(attribute, context, recycledView)
            }
        }
    }

    override fun applyValueToViewForItemList(attribute: OTAttributeDAO, value: Any?, view: View): Single<Boolean> {
        return Single.defer {
            val ratingOptions = getRatingOptions(attribute)
            if (view is StarRatingSlider && value is Fraction) {
                view.score = ratingOptions.convertFractionToRealScore(value)
                view.isFractional = ratingOptions.isFractional
                view.levels = ratingOptions.stars
                Single.just(true)
            } else if (view is StarScoreView && value is Fraction) {
                view.setScore(ratingOptions.convertFractionToRealScore(value), ratingOptions.stars.toFloat())
                Single.just(true)
            } else super.applyValueToViewForItemList(attribute, value, view)
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