package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.ui.IActivityLifeCycle
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.AInputView
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.getActivity
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
abstract class AAttributeInputView<DataType>(layoutId: Int, context: Context, attrs: AttributeSet? = null) : AInputView<DataType?>(layoutId, context, attrs), IActivityLifeCycle, LifecycleObserver {

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

    var boundAttributeObjectId: String? by Delegates.observable(null as String?) {
        prop, old, new ->
        if (old != new) {
            if (new != null) {
                onAttributeBound(new)
            }
        }
    }

    private var activity: AppCompatActivity? = null

    init {
    }

    open fun forceApplyValueAsync(): Single<Nullable<out Any>> {
        return Single.defer {
            clearFocus()
            return@defer Single.just(Nullable(value) as Nullable<Any>)
        }
    }

    var position: Int = -1

    var previewMode: Boolean by Delegates.observable(false) {
        prop, old, new ->
        if (old != new)
            onSetPreviewMode(new)
    }

    protected open fun onSetPreviewMode(mode: Boolean) {
        isEnabled = !mode
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        activity = getActivity()
        activity?.lifecycle?.addObserver(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        activity?.lifecycle?.removeObserver(this)
        activity = null
    }

    /*
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreateLifecycle(){
        println("lifecycle create")
        onCreate(null)
    }*/


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResumeLifecycle() {
        println("lifecycle resume. activity state: ${activity?.lifecycle?.currentState}")
        onResume()
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    open fun onStartLifecycle() {
        println("lifecycle start")
        onStart()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPauseLifecycle() {
        println("lifecycle pause")
        onPause()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    open fun onStopLifecycle() {
        println("lifecycle stop")
        onStop()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroyLifecycle() {
        println("lifecycle destroy")
        onDestroy()
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

    override fun onStart() {
    }

    override fun onStop() {
    }



    protected open fun onAttributeBound(attributeObjectId: String) {

    }

    /***
     * apply value from external activity result
     */
    open fun setValueFromActivityResult(data: Intent, requestType: Int): Boolean {
        return false
    }
}