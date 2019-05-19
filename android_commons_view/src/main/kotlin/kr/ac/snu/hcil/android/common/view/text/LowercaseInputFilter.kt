package kr.ac.snu.hcil.android.common.view.text

import android.text.InputFilter
import android.text.Spanned

class LowercaseInputFilter : InputFilter.AllCaps() {
    override fun filter(source: CharSequence?, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int): CharSequence {
        return source?.toString()?.toLowerCase() ?: ""
    }
}