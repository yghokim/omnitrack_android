package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import android.view.View
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTBooleanProperty
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTSelectionProperty
import kr.ac.snu.hcil.omnitrack.ui.components.common.StarRatingView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.StarRatingInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by younghokim on 16. 9. 6..
 */
class OTRatingAttribute(objectId: String?, dbId: Long?, columnName: String, settingData: String?, connectionData: String?)
: OTAttribute<Float>(objectId, dbId, columnName, OTAttribute.TYPE_RATING, settingData, connectionData) {

    enum class DisplayType(val nameResourceId: Int) {
        Star(R.string.property_rating_display_type_stars),
        Likert(R.string.property_rating_display_type_likert)
    }

    enum class Level(val maxScore: Int) {
        Level5(5), Level7(7), Level10(10)
    }

    companion object {
        const val PROPERTY_DISPLAY_TYPE = 0
        const val PROPERTY_LEVELS = 1
        const val PROPERTY_ALLOW_INTERMEDIATE = 2
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_FLOAT

    override val typeNameResourceId: Int = R.string.type_rating_name

    override val typeSmallIconResourceId: Int get() {
        return when (displayType) {
            DisplayType.Star -> R.drawable.icon_small_star
            DisplayType.Likert -> R.drawable.icon_small_star //TODO add Likert icon
        }
    }

    override val propertyKeys: Array<Int> = arrayOf(PROPERTY_DISPLAY_TYPE, PROPERTY_LEVELS, PROPERTY_ALLOW_INTERMEDIATE)


    override fun createProperties() {
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

    }

    var level: Level get() = Level.values()[getPropertyValue<Int>(PROPERTY_LEVELS)]
        set(value) = setPropertyValue(PROPERTY_LEVELS, value.ordinal)

    var displayType: DisplayType get() = DisplayType.values()[getPropertyValue<Int>(PROPERTY_DISPLAY_TYPE)]
        set(value) = setPropertyValue(PROPERTY_DISPLAY_TYPE, value.ordinal)

    var allowIntermediate: Boolean get() = getPropertyValue(PROPERTY_ALLOW_INTERMEDIATE)
        set(value) = setPropertyValue(PROPERTY_ALLOW_INTERMEDIATE, value)

    override fun formatAttributeValue(value: Any): String {
        return value.toString() + " / ${level.maxScore}"
    }

    override fun getAutoCompleteValueAsync(resultHandler: (Float) -> Unit): Boolean {
        resultHandler.invoke(0f)
        return true
    }

    override fun getInputViewType(previewMode: Boolean): Int {
        return when (displayType) {
            DisplayType.Star -> AAttributeInputView.VIEW_TYPE_RATING_STARS
            DisplayType.Likert -> AAttributeInputView.VIEW_TYPE_RATING_STARS
        }
    }


    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>) {
        if (inputView is StarRatingInputView) {
            inputView.ratingView.allowIntermediate = allowIntermediate
            inputView.ratingView.levels = level.maxScore
            inputView.ratingView.score = level.maxScore / 2.0f
        }
    }

    override fun getViewForItemList(context: Context, recycledView: View?): View {

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

    override fun applyValueToViewForItemList(value: Any?, view: View): Boolean {
        if (view is StarRatingView && value != null) {
            if (value is Float) {
                view.score = value
                view.allowIntermediate = allowIntermediate
                view.levels = level.maxScore
                return true
            } else return false
        } else return super.applyValueToViewForItemList(value, view)
    }

}