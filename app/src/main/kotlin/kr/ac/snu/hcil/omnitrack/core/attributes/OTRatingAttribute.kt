package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import android.view.View
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTRatingOptionsProperty
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.common.StarRatingView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.LikertScaleInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.StarRatingInputView
import kr.ac.snu.hcil.omnitrack.utils.RatingOptions
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable
import rx.Single

/**
 * Created by younghokim on 16. 9. 6..
 */
class OTRatingAttribute(objectId: String?, localKey: Int?, parentTracker: OTTracker?, columnName: String, isRequired: Boolean, settingData: Map<String, Any?>?, connectionData: String?)
    : OTAttribute<Float>(objectId, localKey, parentTracker, columnName, isRequired, OTAttribute.TYPE_RATING, settingData, connectionData) {

    companion object {
        /*
        const val PROPERTY_DISPLAY_TYPE = 0
        const val PROPERTY_LEVELS = 1
        const val PROPERTY_ALLOW_INTERMEDIATE = 2*/
        const val PROPERTY_OPTIONS = "options"
    }

    override val valueNumericCharacteristics: NumericCharacteristics = NumericCharacteristics(true, true)

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_FLOAT

    override val typeNameResourceId: Int = R.string.type_rating_name

    override val typeSmallIconResourceId: Int get() {
        return when (displayType) {
            RatingOptions.DisplayType.Star -> R.drawable.icon_small_star
            RatingOptions.DisplayType.Likert -> R.drawable.icon_small_star //TODO add Likert icon
        }
    }

    override val propertyKeys: Array<String> = arrayOf(PROPERTY_OPTIONS)


    override fun createProperties() {
        assignProperty(OTRatingOptionsProperty(PROPERTY_OPTIONS))

        /*
        assignProperty(OTSelectionProperty(PROPERTY_DISPLAY_TYPE,
                OTApplication.app.resources.getString(R.string.property_rating_display_type),
                DisplayType.values().map { OTApplication.app.resources.getString(it.nameResourceId) }.toTypedArray()
        ))

        assignProperty(OTSelectionProperty(PROPERTY_LEVELS,
                OTApplication.app.resources.getString(R.string.property_rating_levels),
                Level.values().map { it.maxScore.toString() }.toTypedArray()))

        assignProperty(OTBooleanProperty(
                true,
                PROPERTY_ALLOW_INTERMEDIATE,
                OTApplication.app.resources.getString(R.string.property_rating_allow_intermediate)))
*/
    }

    var ratingOptions: RatingOptions get() = getPropertyValue<RatingOptions>(PROPERTY_OPTIONS)
        set(value) {
            setPropertyValue(PROPERTY_OPTIONS, value)
        }

    var level: RatingOptions.StarLevel get() = ratingOptions.starLevels
        set(value) {
            ratingOptions.starLevels = value
        }

    var displayType: RatingOptions.DisplayType get() = ratingOptions.type
        set(value) {
            ratingOptions.type = value
        }

    var allowIntermediate: Boolean get() = ratingOptions.allowIntermediate
        set(value) {
            ratingOptions.allowIntermediate = value
        }

    override fun formatAttributeValue(value: Any): CharSequence {
        return when (displayType) {
            RatingOptions.DisplayType.Star -> value.toString() + " / ${level.maxScore}"
            RatingOptions.DisplayType.Likert -> value.toString()
        }
    }

    override fun compareValues(a: Any, b: Any): Int {
        if (a is Float && b is Float) {
            return a.compareTo(b)
        } else return super.compareValues(a, b)
    }

    override fun getAutoCompleteValue(): Observable<Float> {
        return Observable.just(0f)
    }

    override fun getInputViewType(previewMode: Boolean): Int {
        return when (displayType) {
            RatingOptions.DisplayType.Star -> AAttributeInputView.VIEW_TYPE_RATING_STARS
            RatingOptions.DisplayType.Likert -> AAttributeInputView.VIEW_TYPE_RATING_LIKERT
        }
    }


    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>) {
        if (inputView is StarRatingInputView) {
            inputView.ratingView.allowIntermediate = allowIntermediate
            inputView.ratingView.levels = level.maxScore
            inputView.ratingView.score = level.maxScore / 2.0f
        } else if (inputView is LikertScaleInputView) {
            inputView.scalePicker.allowIntermediate = allowIntermediate
            val options = ratingOptions
            inputView.scalePicker.leftMost = options.leftMost
            inputView.scalePicker.rightMost = options.rightMost
            inputView.scalePicker.leftLabel = options.leftLabel
            inputView.scalePicker.rightLabel = options.rightLabel
            inputView.scalePicker.middleLabel = options.middleLabel
            inputView.scalePicker.value = ((options.rightMost + options.leftMost) shr 1).toFloat()
        }
    }

    override fun getViewForItemList(context: Context, recycledView: View?): View {
        when (displayType) {
            RatingOptions.DisplayType.Star -> {
                val target = if (recycledView is StarRatingView) {
                    recycledView
                } else {
                    StarRatingView(context)
                }

                target.isLightMode = true
                target.overridenIntrinsicWidth = context.resources.getDimensionPixelSize(R.dimen.star_rating_item_list_view_unit_size)
                target.overridenIntrinsicHeight = target.overridenIntrinsicWidth
                return target
            }

            RatingOptions.DisplayType.Likert -> {
                return super.getViewForItemList(context, recycledView)
            }
        }
    }

    override fun applyValueToViewForItemList(value: Any?, view: View): Single<Boolean> {
        return Single.defer {
            if (view is StarRatingView && value != null) {
                if (value is Float) {
                    view.score = value
                    view.allowIntermediate = allowIntermediate
                    view.levels = level.maxScore
                    Single.just(true)
                } else Single.just(false)
            } else super.applyValueToViewForItemList(value, view)
        }
    }

    override fun onAddValueToTable(value: Any?, out: MutableList<String?>) {
        if(value is Float) {
            out.add(formatAttributeValue(value).toString())
        }
        else{
            out.add(null)
        }
    }

}