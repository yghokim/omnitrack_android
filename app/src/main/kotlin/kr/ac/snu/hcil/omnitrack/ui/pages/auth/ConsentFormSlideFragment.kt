package kr.ac.snu.hcil.omnitrack.ui.pages.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import io.noties.markwon.Markwon
import kotlinx.android.synthetic.main.slide_consent_form.view.*
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho on 1/26/2017.
 */
class ConsentFormSlideFragment : SignUpActivity.SlideFragment(SignUpActivity.ESlide.CONSENT_FORM) {

    companion object {
        fun getInstanceFromAsset(path: String): ConsentFormSlideFragment {
            return ConsentFormSlideFragment().apply {
                arguments = bundleOf("assetPath" to path)
            }
        }

        fun getInstanceFromString(markdownString: String): ConsentFormSlideFragment {
            return ConsentFormSlideFragment().apply {
                arguments = bundleOf("markdownString" to markdownString)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.slide_consent_form, container, false)


        /*
        if (arguments?.containsKey("assetPath") == true) {
            markdownView.loadMarkdownFile(
                    "$ANDROID_ASSET_PATH/${arguments?.getString("assetPath")}",
                    "$ANDROID_ASSET_PATH/consent/style.css"
            )
        } else if (arguments?.containsKey("markdownString") == true) {
            markdownView.loadMarkdown(arguments?.getString("markdownString"), "$ANDROID_ASSET_PATH/consent/style.css")
        }*/

        if (arguments?.containsKey("markdownString") == true) {
            val markwon: Markwon = Markwon.create(inflater.context)
            markwon.setMarkdown(view.ui_markdown, arguments?.getString("markdownString") ?: "")
        }

        return view
    }

    override fun onNextTried() {
        goNext()
    }
}