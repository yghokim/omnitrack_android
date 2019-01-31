package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.DialogFragment
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2017-01-26.
 */
class WebviewScreenDialogFragment : DialogFragment() {
    private var webView: WebView? = null
    private var titleView: TextView? = null
    private var exitButton: AppCompatImageButton? = null

    companion object {

        const val KEY_TITLE: String = "title"
        const val KEY_HTML_URL: String = "html_url"


        fun makeInstance(title: String, fileUrl: String): WebviewScreenDialogFragment {
            val args = Bundle()
            args.putString(KEY_TITLE, title)

            args.putString(KEY_HTML_URL, fileUrl)

            val instance = WebviewScreenDialogFragment()
            instance.arguments = args

            instance.setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

            return instance
        }

    }

    private fun initView(view: View) {
        webView = view.findViewById(R.id.webView)
        titleView = view.findViewById(R.id.title)
        exitButton = view.findViewById(R.id.ui_button_cancel)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val inflater = act.layoutInflater
        val view = inflater.inflate(R.layout.activity_webview_screen, null, false)
        initView(view)

        exitButton?.setOnClickListener {
            dismiss()
        }


        val args = arguments

        if (args != null) {
            if (args.containsKey(KEY_TITLE)) {
                titleView?.text = args.getString(KEY_TITLE)
            }


            if (args.containsKey(KEY_HTML_URL)) {
                val webviewFileUrl = args.getString(KEY_HTML_URL)
                if (webviewFileUrl != null) {
                    webView?.loadUrl(webviewFileUrl)
                }
            }
        }

        return AlertDialog.Builder(act)
                .setView(view)
                .setCancelable(true)
                .create()
    }
}