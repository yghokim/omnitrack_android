package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipboardManager
import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-09-02
 */
object TextInputDialogHelper {

    fun makeDialog(context: Context, title: String, hint: String, pasteClipBoardFirst: Boolean = false, onOk: (CharSequence) -> Unit, onCancel: (() -> Unit)? = null): Dialog {
        val view = View.inflate(context, R.layout.dialog_text_input, null)

        val textInput = view.findViewById(R.id.editText) as TextView
        textInput.hint = hint

        if (pasteClipBoardFirst) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription.hasMimeType("text/plain") && clipboard.primaryClip.itemCount > 0) {
                textInput.text = clipboard.primaryClip.getItemAt(0).text
            }
        }

        textInput.setOnEditorActionListener { textView, keyCode, event ->
            if ((event.action == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                onOk.invoke(textInput.text)
                true
            } else false
        }

        return AlertDialog.Builder(context)
                .setView(view)
                .setTitle(title)
                .setCancelable(true)
                .setPositiveButton(R.string.msg_ok) {
                    a, b ->
                    onOk.invoke(textInput.text)
                }
                .setOnCancelListener {
                    a ->
                    onCancel?.invoke()
                }
                .create()
    }

    fun makeDialog(context: Context, titleResId: Int, hintResId: Int, pasteClipBoardFirst: Boolean = false, onOk: (CharSequence) -> Unit, onCancel: (() -> Unit)? = null): Dialog {
        return makeDialog(context, context.resources.getString(titleResId), context.resources.getString(hintResId), pasteClipBoardFirst, onOk, onCancel)
    }


}