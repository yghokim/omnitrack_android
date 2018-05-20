package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.component_reminder_action_config_panel.view.*
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTReminderAction
import kr.ac.snu.hcil.omnitrack.utils.setPaddingBottom
import kr.ac.snu.hcil.omnitrack.utils.setPaddingLeft
import kr.ac.snu.hcil.omnitrack.utils.setPaddingRight

class ReminderActionConfigurationPanel : ConstraintLayout {

    companion object {
        const val EXPIRY_UNIT_HOUR = 0
        const val EXPIRY_UNIT_MINUTE = 1
        const val EXPIRY_UNIT_SECOND = 2

        fun getExpiryMultiplier(unit: Int): Int = when (unit) {
            EXPIRY_UNIT_HOUR -> 360
            EXPIRY_UNIT_MINUTE -> 60
            EXPIRY_UNIT_SECOND -> 1
            else -> 1
        }
    }

    var expiry: Int
        get() = if (!ui_use_reminder_expiry.value) {
            OTReminderAction.EXPIRY_INDEFINITE
        } else ui_expiry_digit_input.text.toString().toInt() * getExpiryMultiplier(ui_expiry_unit_spinner.selectedIndex)
        set(value) {
            if (value == OTReminderAction.EXPIRY_INDEFINITE || value < 0) {
                ui_use_reminder_expiry.value = false
            } else {
                ui_use_reminder_expiry.value = true
                var idealUnit: Int = EXPIRY_UNIT_SECOND
                for (unit in 0..2) {
                    val multiplier = getExpiryMultiplier(unit)
                    if (value / multiplier > 0 && (value / multiplier) * multiplier == value) {
                        idealUnit = unit
                        break
                    }
                }
                ui_expiry_unit_spinner.selectedIndex = idealUnit
                ui_expiry_digit_input.setText((value / getExpiryMultiplier(idealUnit)).toString(), TextView.BufferType.NORMAL)
            }
            refreshUi()
        }

    private val _expirySubject = PublishSubject.create<Int>()
    val expirySubject: Subject<Int> get() = _expirySubject

    private val timeUnits: Array<String> = arrayOf(OTApp.getString(R.string.time_duration_hour_full), OTApp.getString(R.string.time_duration_minute_full), OTApp.getString(R.string.time_duration_second_full))

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.component_reminder_action_config_panel, this, true)

        setPaddingLeft(resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin))
        setPaddingRight(resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin))
        setPaddingBottom(resources.getDimensionPixelSize(R.dimen.activity_vertical_margin))


        ui_expiry_unit_spinner.setItems(*timeUnits)
        ui_expiry_unit_spinner.selectedIndex = EXPIRY_UNIT_MINUTE

        ui_expiry_digit_input.setText("5", TextView.BufferType.NORMAL)

        ui_use_reminder_expiry.valueChanged += { sender, useExpiry ->
            refreshUi()
            _expirySubject.onNext(expiry)
        }

        ui_use_reminder_expiry.value = false
        ui_expiry_title.isEnabled = false
        ui_expiry_title.alpha = 0.3f
        ui_expiry_unit_spinner.isEnabled = false
        ui_expiry_unit_spinner.alpha = 0.3f
        ui_expiry_digit_input.isEnabled = false
        ui_expiry_digit_input.alpha = 0.3f

        ui_expiry_unit_spinner.setOnItemSelectedListener { view, position, id, item ->
            _expirySubject.onNext(expiry)
        }

        ui_expiry_digit_input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (s.toString() == "0" || s.toString().isBlank()) {
                    ui_expiry_digit_input.setText("1", TextView.BufferType.NORMAL)
                } else {
                    _expirySubject.onNext(expiry)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })
    }

    private fun refreshUi() {
        if (ui_use_reminder_expiry.value) {
            ui_expiry_title.isEnabled = true
            ui_expiry_title.alpha = 1.0f
            ui_expiry_unit_spinner.isEnabled = true
            ui_expiry_unit_spinner.alpha = 1.0f
            ui_expiry_digit_input.isEnabled = true
            ui_expiry_digit_input.alpha = 1.0f
        } else {
            ui_expiry_title.isEnabled = false
            ui_expiry_title.alpha = 0.3f
            ui_expiry_unit_spinner.isEnabled = false
            ui_expiry_unit_spinner.alpha = 0.3f
            ui_expiry_digit_input.isEnabled = false
            ui_expiry_digit_input.alpha = 0.3f
        }
    }
}