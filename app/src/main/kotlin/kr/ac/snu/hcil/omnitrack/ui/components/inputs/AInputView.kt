package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-13.
 */
abstract class AInputView<T>(layoutId: Int, context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    val valueChanged = Event<T>()

    private val validators: ArrayList<Pair<CharSequence?, (T) -> Boolean>> = ArrayList<Pair<CharSequence?, (T) -> Boolean>>()

    protected val validationErrorMessageList = ArrayList<CharSequence>()

    constructor(layoutId: Int, context: Context): this(layoutId, context, null)

    init{
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        try {
            addView(inflater.inflate(layoutId, this, false))
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    fun addNewValidator(failedMessage: CharSequence?, func: (T)->Boolean){
        validators.add(Pair<CharSequence?, (T)->Boolean>(failedMessage, func))
    }

    fun validate(): Boolean {
        return validate(value)
    }

    fun validate(value: T): Boolean{
        validationErrorMessageList.clear()

        var passed = true
        for(entry in validators)
        {
            if(entry.second(value) == false)
            {
                passed = false
                if( entry.first != null) {
                    validationErrorMessageList.add(entry.first!!)
                }
            }
        }

        onValidated(passed)
        return passed
    }

    open fun onValidated(result: Boolean)
    {
        ;
    }

    protected fun onValueChanged(newValue: T){
        validate()
        valueChanged.invoke(this, newValue)
    }

    abstract var value : T

    abstract fun focus(): Unit
}