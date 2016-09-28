package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.ui.IActivityLifeCycle
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.AInputView
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
abstract class AAttributeInputView<DataType>(layoutId: Int, context: Context, attrs: AttributeSet? = null) : AInputView<DataType>(layoutId, context, attrs), IActivityLifeCycle {


    companion object {
        const val VIEW_TYPE_NUMBER = 0
        const val VIEW_TYPE_TIME_POINT = 1
        const val VIEW_TYPE_LONG_TEXT = 2
        const val VIEW_TYPE_SHORT_TEXT = 3
        const val VIEW_TYPE_LOCATION = 4
        const val VIEW_TYPE_TIME_RANGE_PICKER = 5
        const val VIEW_TYPE_CHOICE = 7
        const val VIEW_TYPE_RATING_STARS = 8
        const val VIEW_TYPE_RATING_LIKERT = 9
        const val VIEW_TYPE_IMAGE = 10
        const val VIEW_TYPE_AUDIO_RECORD = 11



        fun makeInstance(type: Int, context: Context): AAttributeInputView<out Any> {
            return when (type) {
                VIEW_TYPE_NUMBER -> NumberInputView(context)
                VIEW_TYPE_TIME_POINT -> TimePointInputView(context)
                VIEW_TYPE_LONG_TEXT -> LongTextInputView(context)
                VIEW_TYPE_SHORT_TEXT -> ShortTextInputView(context)
                VIEW_TYPE_LOCATION -> LocationInputView(context)
                VIEW_TYPE_TIME_RANGE_PICKER -> TimeRangePickerInputView(context)
                VIEW_TYPE_CHOICE -> ChoiceInputView(context)
                VIEW_TYPE_RATING_STARS -> StarRatingInputView(context)
                VIEW_TYPE_RATING_LIKERT -> LikertScaleInputView(context)
                VIEW_TYPE_IMAGE -> ImageInputView(context)
                VIEW_TYPE_AUDIO_RECORD -> AudioRecordInputView(context)
                else -> throw IllegalArgumentException("attribute view data type ${type} is not supported yet.")
            }
        }

        fun makeActivityForResultRequestCode(position: Int, requestType: Int): Int {
            //key is stored in upper 8 bit, type in under 8 bit
            return (position shl 8) or requestType
        }

        fun getPositionFromRequestCode(requestCode: Int): Int {
            return (requestCode shr 8) and 0xFF
        }

        fun getRequestTypeFromRequestCode(requestCode: Int): Int {
            return requestCode and 0xFF
        }
    }

    abstract val typeId: Int
        get

    var boundAttributeId: String? by Delegates.observable(null as String?) {
        prop, old, new ->
        if (old != new) {
            if (new != null) {
                onAttributeBound(new)
            }
        }
    }

    init {
    }

    var position: Int = -1

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

    override fun onCreate(savedInstanceState: Bundle?) {
    }

    override fun onSaveInstanceState(outState: Bundle?) {
    }

    override fun onResume() {
    }

    override fun onPause() {
    }

    override fun onDestroy() {
    }

    override fun onLowMemory() {
    }

    protected open fun onAttributeBound(attributeId: String) {

    }

    /***
     * apply value from external activity result
     */
    open fun setValueFromActivityResult(data: Intent, requestType: Int): Boolean {
        return false
    }
}