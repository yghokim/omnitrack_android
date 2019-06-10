package kr.ac.snu.hcil.omnitrack.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import kr.ac.snu.hcil.android.common.events.Event
import kr.ac.snu.hcil.android.common.view.container.LockableFrameLayout
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-13.
 */
abstract class AInputView<T>(layoutId: Int, context: Context, attrs: AttributeSet?) : LockableFrameLayout(context, attrs) {

    val valueChanged = Event<T>()

    val validationFailed = Event<String>()

    private val validators: ArrayList<Pair<CharSequence?, (T) -> Boolean>> = ArrayList()

    protected val validationErrorMessageList = ArrayList<CharSequence>()

    abstract var value: T

    constructor(layoutId: Int, context: Context) : this(layoutId, context, null) {
        locked = !isEnabled
    }

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

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        this.locked = !enabled
    }

    override fun onViewLocked() {
        super.onViewLocked()
        this.alpha = 0.5f
    }

    override fun onViewUnlocked() {
        super.onViewUnlocked()
        this.alpha = 1.0f
    }

    fun addNewValidator(failedMessage: CharSequence?, func: (T) -> Boolean) {
        validators.add(Pair(failedMessage, func))
    }

    fun validate(): Boolean {
        return validate(value)
    }

    fun validate(value: T): Boolean {
        validationErrorMessageList.clear()

        var passed = true
        for (entry in validators) {
            if (!entry.second(value)) {
                passed = false
                val msg = entry.first
                if (msg != null) {
                    validationErrorMessageList.add(msg)
                }
            }
        }

        if (!passed) {
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
    open fun setAnyValue(value: Any?) {
        this.value = value as T
    }

    abstract fun focus()
}