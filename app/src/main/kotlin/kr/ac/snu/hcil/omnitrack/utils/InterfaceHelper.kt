package kr.ac.snu.hcil.omnitrack.utils

import android.content.Context
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

/**
 * Created by Young-Ho Kim on 2016-07-25.
 */
object InterfaceHelper {
    fun makeEditTextDynamicallyShowCursor(view: EditText) {
        view.setOnClickListener {
            v ->
            view.isCursorVisible = true
        }

        view.setOnEditorActionListener { textView, i, event ->

            view.isCursorVisible = false
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                val `in` = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                `in`.hideSoftInputFromWindow(view.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS)
            }

            false
        }
    }
}