package kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.factories

import android.content.Context
import android.view.View
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTRatingFieldHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.types.Fraction
import kr.ac.snu.hcil.omnitrack.core.types.RatingOptions
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.OTFieldViewFactory
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.LikertScaleInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.StarRatingInputView
import kr.ac.snu.hcil.omnitrack.views.rating.StarRatingSlider
import kr.ac.snu.hcil.omnitrack.views.rating.StarScoreView

class RatingViewFactory(helper: OTRatingFieldHelper) : OTFieldViewFactory<OTRatingFieldHelper>(helper) {

    override fun getInputViewType(previewMode: Boolean, field: OTFieldDAO): Int {
        return when (helper.getRatingOptions(field).type) {
            RatingOptions.DisplayType.Star -> AFieldInputView.VIEW_TYPE_RATING_STARS
            RatingOptions.DisplayType.Likert -> AFieldInputView.VIEW_TYPE_RATING_LIKERT
        }
    }

    override fun refreshInputViewUI(inputView: AFieldInputView<out Any>, field: OTFieldDAO) {
        val options = helper.getRatingOptions(field)
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

    override fun getViewForItemList(field: OTFieldDAO, context: Context, recycledView: View?): View {
        val ratingOptions = helper.getRatingOptions(field)

        when (ratingOptions.type) {
            RatingOptions.DisplayType.Star -> {

                if (ratingOptions.stars <= 5) {
                    val target = recycledView as? StarRatingSlider ?: StarRatingSlider(context)

                    target.isLightMode = true
                    target.overridenIntrinsicWidth = context.resources.getDimensionPixelSize(R.dimen.star_rating_item_list_view_unit_size)
                    target.overridenIntrinsicHeight = target.overridenIntrinsicWidth
                    return target
                } else {
                    return recycledView as? StarScoreView ?: StarScoreView(context)
                }
            }

            RatingOptions.DisplayType.Likert -> {
                return super.getViewForItemList(field, context, recycledView)
            }
        }
    }

    override fun applyValueToViewForItemList(context: Context, field: OTFieldDAO, value: Any?, view: View): Single<Boolean> {
        return Single.defer {
            val ratingOptions = helper.getRatingOptions(field)
            if (view is StarRatingSlider && value is Fraction) {
                view.score = ratingOptions.convertFractionToRealScore(value)
                view.isFractional = ratingOptions.isFractional
                view.levels = ratingOptions.stars
                Single.just(true)
            } else if (view is StarScoreView && value is Fraction) {
                view.setScore(ratingOptions.convertFractionToRealScore(value), ratingOptions.stars.toFloat())
                Single.just(true)
            } else super.applyValueToViewForItemList(context, field, value, view)
        }
    }
}