package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.LikertScaleInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.StarRatingInputView
import kr.ac.snu.hcil.omnitrack.utils.RatingOptions
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTRatingAttributeHelper : OTAttributeHelper() {

    companion object {
        const val PROPERTY_OPTIONS = "options"
    }

    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics = NumericCharacteristics(true, true)

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int {
        return R.string.type_rating_name
    }

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int {
        return R.drawable.icon_small_star //TODO Options
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_FLOAT

    override val propertyKeys: Array<String> = arrayOf(PROPERTY_OPTIONS)

    override fun <T> parsePropertyValue(propertyKey: String, serializedValue: String): T? {
        return when (propertyKey) {
            PROPERTY_OPTIONS -> OTPropertyManager.getHelper(OTPropertyManager.EPropertyType.RatingOptions).parseValue(serializedValue)
            else -> throw IllegalArgumentException("Unsupported property type: ${propertyKey}")
        } as T
    }

    fun getRatingOptions(attribute: OTAttributeDAO): RatingOptions {
        return getDeserializedPropertyValue<RatingOptions>(PROPERTY_OPTIONS, attribute) ?: RatingOptions()
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
            inputView.ratingView.allowIntermediate = options.allowIntermediate
            inputView.ratingView.levels = options.starLevels.maxScore
            inputView.ratingView.score = options.starLevels.maxScore / 2.0f
        } else if (inputView is LikertScaleInputView) {
            inputView.scalePicker.allowIntermediate = options.allowIntermediate
            inputView.scalePicker.leftMost = options.leftMost
            inputView.scalePicker.rightMost = options.rightMost
            inputView.scalePicker.leftLabel = options.leftLabel
            inputView.scalePicker.rightLabel = options.rightLabel
            inputView.scalePicker.middleLabel = options.middleLabel
            inputView.scalePicker.value = ((options.rightMost + options.leftMost) shr 1).toFloat()
        }
    }
}