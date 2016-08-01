package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.AInputView
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
abstract class AAttributeInputView<DataType>(layoutId: Int, context: Context, attrs: AttributeSet? = null) : AInputView<DataType>(layoutId, context, attrs) {

    companion object {
        const val VIEW_TYPE_NUMBER = 0
        const val VIEW_TYPE_TIME_POINT = 1
        const val VIEW_TYPE_LONG_TEXT = 2
        const val VIEW_TYPE_SHORT_TEXT = 3


        fun makeInstance(type: Int, context: Context): AAttributeInputView<out Any> {
            return when (type) {
                VIEW_TYPE_NUMBER -> NumberInputView(context)
                VIEW_TYPE_TIME_POINT -> TimePointInputView(context)
                VIEW_TYPE_LONG_TEXT -> LongTextInputView(context)
                VIEW_TYPE_SHORT_TEXT -> ShortTextInputView(context)
                else -> throw IllegalArgumentException("attribute view data type ${type} is not supported yet.")
            }
        }
    }

    abstract val typeId: Int
        get

    init {
    }

    var previewMode: Boolean by Delegates.observable(false) {
        prop, old, new ->
        if (old != new)
            onSetPreviewMode(new)
    }

    protected open fun onSetPreviewMode(mode: Boolean) {
        if (mode) {
            isEnabled = false
            //alpha = 0.5f
        } else {
            isEnabled = true
            //alpha = 1.0f
        }
    }
}