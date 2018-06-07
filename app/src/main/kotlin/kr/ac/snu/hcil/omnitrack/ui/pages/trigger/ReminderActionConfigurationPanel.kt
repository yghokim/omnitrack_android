package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.component_reminder_action_config_panel.view.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTReminderAction
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.setPaddingBottom
import kr.ac.snu.hcil.omnitrack.utils.setPaddingLeft
import kr.ac.snu.hcil.omnitrack.utils.setPaddingRight

class ReminderActionConfigurationPanel : ConstraintLayout {

    var expiry: Int
        get() = if (!ui_use_reminder_expiry.isChecked) {
            OTReminderAction.EXPIRY_INDEFINITE
        } else ui_expiry_duration_picker.durationSeconds
        set(value) {
            if (value == OTReminderAction.EXPIRY_INDEFINITE || value < 0) {
                ui_use_reminder_expiry.isChecked = false
            } else {
                ui_use_reminder_expiry.isChecked = true
                ui_expiry_duration_picker.durationSeconds = value
            }
        }

    var message: String?
        get() = ui_reminder_message_input.value.let {
            if (it.isBlank()) {
                return null
            } else return it
        }
        set(value) {
            ui_reminder_message_input.value = value ?: ""
        }

    private val _expirySubject = PublishSubject.create<Int>()
    val expirySubject: Subject<Int> get() = _expirySubject

    private val _messageSubject = PublishSubject.create<Nullable<String>>()
    val messageSubject: Subject<Nullable<String>> get() = _messageSubject

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.component_reminder_action_config_panel, this, true)

        setPaddingLeft(resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin))
        setPaddingRight(resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin))
        setPaddingBottom(resources.getDimensionPixelSize(R.dimen.activity_vertical_margin))

        ui_reminder_message_input.hint = "Use system default message"
        ui_reminder_message_input.inputLengthMin = 0
        ui_reminder_message_input.inputLengthMax = 30
        ui_reminder_message_input.dialogTitle = "Insert custom message"


        ui_use_reminder_expiry.isChecked = true
        ui_expiry_duration_picker.isEnabled = true

        ui_use_reminder_expiry.setOnCheckedChangeListener { buttonView, isChecked ->
            ui_expiry_duration_picker.isEnabled = isChecked
            _expirySubject.onNext(expiry)
        }

        ui_expiry_duration_picker.durationChanged += { sender, duration ->
            _expirySubject.onNext(duration)
        }

        ui_reminder_message_input.valueChanged += { sender, message ->
            _messageSubject.onNext(Nullable(this.message))
        }
    }
}