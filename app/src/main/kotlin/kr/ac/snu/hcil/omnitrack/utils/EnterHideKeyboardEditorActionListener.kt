package kr.ac.snu.hcil.omnitrack.utils

import android.content.Context
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView


/**
 * Created by Young-Ho Kim on 2016-12-07.
 */
open class EnterHideKeyboardEditorActionListener : TextView.OnEditorActionListener {

    init {
    }

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            //Toast.makeText(getActivity(), "call",45).show();
            // hide virtual keyboard
            val imm = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
            v.clearFocus()
            return true
        }
        return false
    }

}