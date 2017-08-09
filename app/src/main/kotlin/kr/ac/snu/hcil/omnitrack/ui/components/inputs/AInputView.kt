package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import kr.ac.snu.hcil.omnitrack.ui.components.common.LockableFrameLayout
import kr.ac.snu.hcil.omnitrack.utils.ReadOnlyPair
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-13.
 */
abstract class AInputView<T>(layoutId: Int, context: Context, attrs: AttributeSet?) : LockableFrameLayout(context, attrs) {

    val valueChanged = Event<T>()

    val validationFailed = Event<String>()

    private val validators: ArrayList<ReadOnlyPair<CharSequence?, (T) -> Boolean>> = ArrayList<ReadOnlyPair<CharSequence?, (T) -> Boolean>>()

    protected val validationErrorMessageList = ArrayList<CharSequence>()

    abstract var value: T

    constructor(layoutId: Int, context: Context) : this(layoutId, context, null)

    init {
        if (layoutId != 0) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            try {
                inflater.inflate(layoutId, this, true)
            } catch(e: Exception) {
                e.printStackTrace()
                throw Exception("Inflation failed")
            }
        }
    }

    fun addNewValidator(failedMessage: CharSequence?, func: (T) -> Boolean) {
        validators.add(ReadOnlyPair<CharSequence?, (T) -> Boolean>(failedMessage, func))
    }

    fun validate(): Boolean {
        return validate(value)
    }

    fun validate(value: T): Boolean {
        validationErrorMessageList.clear()

        var passed = true
        for (entry in validators) {
            if (entry.second(value) == false) {
                passed = false
                val msg = entry.first
                if (msg != null) {
                    validationErrorMessageList.add(msg)
                }
            }
        }

        if (passed == false) {
            validationFailed.invoke(this, validationErrorMessageList.joinToString("\n"))
        }
        onValidated(passed)
        return passed
    }

    open fun onValidated(result: Boolean) {
    }

    protected open fun onValueChanged(newValue: T) {
        validate()
        valueChanged.invoke(this, newValue)
    }

    @Suppress("UNCHECKED_CAST")
    open fun setAnyValue(value: Any) {
        this.value = value as T
    }

    abstract fun focus(): Unit
}