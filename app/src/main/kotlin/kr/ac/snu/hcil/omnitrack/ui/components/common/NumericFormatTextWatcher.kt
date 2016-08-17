package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.DecimalFormat
import java.text.ParseException

/**
 * Created by Young-Ho Kim on 2016-07-25.
 * http://blog.roshka.com/2012/08/android-edittext-with-number-format.html
 */
class NumericFormatTextWatcher(private val et: EditText) : TextWatcher {

    companion object {
        const val UNIT_POSITOIN_NONE = 0
        const val UNIT_POSITOIN_FRONT = 1
        const val UNIT_POSITOIN_END = 2
    }

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

    private var hasFractionalPart: Boolean = false

    private lateinit var format: DecimalFormat

    private fun updateFormat() {
        format.groupingSize = commasPerDigit
        format.isGroupingUsed = commasPerDigit > 0
        format.maximumFractionDigits = numDigitsUnderPoint
        format.isDecimalSeparatorAlwaysShown = numDigitsUnderPoint > 0
    }

    override fun afterTextChanged(s: Editable?) {
        et.removeTextChangedListener(this)

        try {
            val inilen: Int
            val endlen: Int
            inilen = et.text.length

            val v = s.toString().replace(format.decimalFormatSymbols.groupingSeparator.toString(), "")
            val n = format.parse(v)
            val cp = et.selectionStart
            /*
            if (hasFractionalPart) {
                et.setText(df.format(n))
            } else {
                et.setText(dfnd.format(n))
            }*/
            et.setText(format.format(n))

            endlen = et.text.length
            val sel = cp + (endlen - inilen)
            if (sel > 0 && sel <= et.text.length) {
                et.setSelection(sel)
            } else {
                // place cursor at the end?
                et.setSelection(et.text.length - 1)
            }
        } catch (nfe: NumberFormatException) {
            // do nothing?
        } catch (e: ParseException) {
            // do nothing?
        }

        et.addTextChangedListener(this)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (s.toString().contains(format.decimalFormatSymbols.decimalSeparator)) {
            hasFractionalPart = true
        } else {
            hasFractionalPart = false
        }
    }

    init {
        updateFormat()
    }
}