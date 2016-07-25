package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.components.NumericFormatTextWatcher
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.ParseException
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
class NumberInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<Double>(R.layout.input_number, context, attrs) {
    companion object {
        const val UNIT_POSITOIN_NONE = 0
        const val UNIT_POSITOIN_FRONT = 1
        const val UNIT_POSITOIN_END = 2
    }

    override val typeId: Int = VIEW_TYPE_NUMBER
    override var value: Double = 0.0
        set(value) {
            if (field != value) {
                field = value
                applyValueToView()
            }
        }

    private var moveUnit: Int = 1

    private lateinit var increaseButton: View
    private lateinit var decreaseButton: View
    private lateinit var valueField: EditText
    private lateinit var valueStatic: TextView
    var numDigitsUnderPoint: Int = 3
        set(value) {
            if (field != value) {
                field = value
                updateFormat()
            }
        }

    var commasPerDigit: Int = 3
        set(value) {
            if (field != value) {
                field = value
                updateFormat()
            }
        }

    var unitText: String = ""
        set(value) {
            if (field != value) {
                field = value
                updateFormat()
            }
        }

    var unitPosition: Int = UNIT_POSITOIN_NONE
        set(value) {
            if (field != value) {
                field = value
                updateFormat()
            }
        }

    var typingMode: Boolean by Delegates.observable(false)
    {
        prop, old, new ->
        if (old != new) {
            if (new) {
                applyValueToView()
                valueStatic.visibility = GONE
                valueField.visibility = VISIBLE
                valueField.requestFocus()
            } else {

                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(valueField.windowToken, 0)
                valueStatic.visibility = VISIBLE
                valueField.visibility = GONE
            }

        }
    }

    private var hasFractionalPart: Boolean = false

    private val format = DecimalFormat("#,###.###")

    private val onButtonClickedHandler = {
        view: View ->
        if (typingMode) {
            applyFieldValue()
            typingMode = false
        }
        when (view.id) {
            R.id.ui_button_plus ->
                value += moveUnit
            R.id.ui_button_minus ->
                value -= moveUnit
        }
    }

    init {
        increaseButton = findViewById(R.id.ui_button_plus)
        decreaseButton = findViewById(R.id.ui_button_minus)

        valueField = findViewById(R.id.valueField) as EditText
        valueStatic = findViewById(R.id.valueStatic) as TextView

        valueField.visibility = GONE

        valueStatic.setOnClickListener {
            view ->
            typingMode = true
        }

        valueField.setOnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                applyFieldValue()
                typingMode = false
            }
        }

        valueField.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_DONE || keyEvent?.action == KeyEvent.ACTION_DOWN && keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER) {
                applyFieldValue()
                typingMode = false
                true
            } else false
        }

        increaseButton.setOnClickListener(onButtonClickedHandler)
        decreaseButton.setOnClickListener(onButtonClickedHandler)

        updateFormat()

        applyValueToView()
    }

    fun applyValueToView() {
        valueStatic.text = format.format(value)
        var bigDecimalString = BigDecimal(value).toPlainString()
        val decimalPointIndex = bigDecimalString.indexOfFirst { it == '.' }
        if (decimalPointIndex != -1) {
            bigDecimalString = bigDecimalString.substring(0, Math.min(decimalPointIndex + numDigitsUnderPoint + 1, bigDecimalString.length))
        }

        if (bigDecimalString.last() == '.') {
            bigDecimalString.substring(0, bigDecimalString.length)
        }

        valueField.setText(bigDecimalString)
    }

    fun applyFieldValue() {
        try {
            value = valueField.text.toString().toDouble()
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateFormat() {
/*
        format.groupingSize = commasPerDigit
        format.maximumFractionDigits = numDigitsUnderPoint
        format.isDecimalSeparatorAlwaysShown = if(commasPerDigit > 0){true}else{false}
*/
        format.roundingMode = RoundingMode.CEILING
    }

    override fun focus() {

    }

}