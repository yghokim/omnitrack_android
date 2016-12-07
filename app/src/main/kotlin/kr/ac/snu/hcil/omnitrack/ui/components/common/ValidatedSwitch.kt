package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.support.v7.widget.SwitchCompat
import android.util.AttributeSet
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-08-12.
 */
class ValidatedSwitch : SwitchCompat {

    interface IValidationListener {
        fun onValidationFailed(switch: SwitchCompat, on: Boolean)
        fun onValidationSucceeded(switch: SwitchCompat, on: Boolean)
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var switchOffValidator: (() -> Boolean)? = null
    var switchOnValidator: (() -> Boolean)? = null

    private val listeners = ArrayList<IValidationListener>()

    fun addValidationListener(listener: IValidationListener) {
        this.listeners.add(listener)
    }

    override fun toggle() {
        if (isChecked) {
            if (switchOffValidator?.invoke() ?: true) {
                super.toggle()
                for (listener in listeners) {
                    listener.onValidationSucceeded(this, false)
                }
            } else {

                for (listener in listeners) {
                    listener.onValidationFailed(this, false)
                }
            }
        } else {
            if (switchOnValidator?.invoke() ?: true) {
                super.toggle()

                for (listener in listeners) {
                    listener.onValidationSucceeded(this, true)
                }
            } else {

                for (listener in listeners) {
                    listener.onValidationFailed(this, true)
                }
            }
        }
    }
}