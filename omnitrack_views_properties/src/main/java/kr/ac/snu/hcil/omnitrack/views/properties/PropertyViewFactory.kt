package kr.ac.snu.hcil.omnitrack.views.properties

import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.*

object PropertyViewFactory {
    fun <T> makeView(propertyHelper: OTPropertyHelper<T>, context: Context): APropertyView<T> {
        return when (propertyHelper) {
            is OTBooleanPropertyHelper -> BooleanPropertyView(context, null)
            is OTChoiceEntryListPropertyHelper -> ChoiceEntryListPropertyView(context, null)
            is OTNumberPropertyHelper -> NumberPropertyView(context, null)
            is OTNumberStylePropertyHelper -> NumberStylePropertyView(context, null)
            is OTRatingOptionsPropertyHelper -> RatingOptionsPropertyView(context, null)
            is OTSelectionPropertyHelper -> SelectionPropertyView(context, null)
            else -> throw UnsupportedOperationException("this property was not implemented as view.")
        } as APropertyView<T>
    }
}