package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.android.synthetic.main.component_property_modaltext.view.*
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by younghokim on 2017. 11. 10..
 */
class ModalTextPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<String>(R.layout.component_property_modaltext, context, attrs), View.OnClickListener {

    private val editTextDialog: MaterialDialog by lazy {
        MaterialDialog.Builder(context)
                .title(String.format(OTApp.getString(R.string.msg_format_change_name), OTApp.getString(R.string.msg_text_tracker)))
                .inputType(InputType.TYPE_CLASS_TEXT)
                .inputRangeRes(1, 20, R.color.colorRed)
                .cancelable(true)
                .negativeText(R.string.msg_cancel)
                .input(null, value, false) { dialog, input ->
                    if (value != input) {
                        val string = input.toString()
                        value = string
                        onValueChanged(string)
                    }
                }.build()
    }

    override var value: String
        get() = ui_text.text.toString()
        set(value) {
            if (ui_text.text.toString() != value) {
                ui_text.text = value
            }
        }

    init {
        ui_text.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        if (view === ui_text) {
            editTextDialog.show()
        }
    }


    override fun getSerializedValue(): String? {
        return value
    }

    override fun setSerializedValue(serialized: String): Boolean {
        try {
            value = serialized
            return true
        } catch (e: Exception) {
            return false
        }
    }

    override fun focus() {

    }
}