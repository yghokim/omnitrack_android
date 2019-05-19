package kr.ac.snu.hcil.omnitrack.ui.pages.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.core.os.bundleOf
import kr.ac.snu.hcil.android.common.ANDROID_ASSET_PATH
import kr.ac.snu.hcil.omnitrack.R
import us.feras.mdv.MarkdownView

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

    private lateinit var markdownView: MarkdownView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.slide_consent_form, container, false)
        markdownView = view.findViewById(R.id.ui_markdown)

        if (arguments?.containsKey("assetPath") == true) {
            markdownView.loadMarkdownFile(
                    "$ANDROID_ASSET_PATH/${arguments?.getString("assetPath")}",
                    "$ANDROID_ASSET_PATH/consent/style.css"
            )
        } else if (arguments?.containsKey("markdownString") == true) {
            markdownView.loadMarkdown(arguments?.getString("markdownString"), "$ANDROID_ASSET_PATH/consent/style.css")
        }

        markdownView.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
        markdownView.isScrollbarFadingEnabled = false

        return view
    }

    override fun onNextTried() {
        goNext()
    }
}