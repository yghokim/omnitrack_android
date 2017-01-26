package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.support.v7.widget.AppCompatImageButton
import android.view.View
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import us.feras.mdv.MarkdownView

/**
 * Created by Young-Ho Kim on 2017-01-26.
 */
class MarkdownScreenDialogFragment : DialogFragment() {
    private var markdownView: MarkdownView? = null
    private var titleView: TextView? = null
    private var exitButton: AppCompatImageButton? = null

    companion object {

        const val KEY_TITLE: String = "title"
        const val KEY_MARKDOWN_URL: String = "markdown_url"
        const val KEY_CSS_URL: String = "css_url"
        const val KEY_MARKDOWN_TEXT: String = "markdown_text"


        fun makeInstance(title: String, markdownFileUrl: String, cssUrl: String? = null): MarkdownScreenDialogFragment {
            val args = Bundle()
            args.putString(KEY_TITLE, title)

            args.putString(KEY_MARKDOWN_URL, markdownFileUrl)

            if (cssUrl != null)
                args.putString(KEY_CSS_URL, cssUrl)

            val instance = MarkdownScreenDialogFragment()
            instance.arguments = args

            instance.setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

            return instance
        }

    }

    private fun initView(view: View) {
        markdownView = view.findViewById(R.id.mdView) as MarkdownView
        titleView = view.findViewById(R.id.title) as TextView
        exitButton = view.findViewById(R.id.ui_button_cancel) as AppCompatImageButton
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val inflater = activity.layoutInflater
        val view = inflater.inflate(R.layout.activity_markdown_screen, null)
        initView(view)

        exitButton?.setOnClickListener {
            dismiss()
        }


        val args = arguments

        if (args != null) {
            if (args.containsKey(KEY_TITLE)) {
                titleView?.text = args.getString(KEY_TITLE)
            }

            val markdownCssUrl = if (args.containsKey(KEY_CSS_URL)) {
                args.getString(KEY_CSS_URL)
            } else null

            if (args.containsKey(KEY_MARKDOWN_URL)) {
                val markdownFileUrl = args.getString(KEY_MARKDOWN_URL)
                if (markdownCssUrl != null) {
                    markdownView?.loadMarkdownFile(markdownFileUrl, markdownCssUrl)
                } else markdownView?.loadMarkdownFile(markdownFileUrl)
            } else if (args.containsKey(KEY_MARKDOWN_TEXT)) {
                val markdownString = args.getString(KEY_MARKDOWN_TEXT)
                if (markdownCssUrl != null) {
                    markdownView?.loadMarkdown(markdownString, markdownCssUrl)
                } else {
                    markdownView?.loadMarkdown(markdownString)
                }
            }
        }

        return AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(true)
                .create()
    }
}