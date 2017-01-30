package kr.ac.snu.hcil.omnitrack.ui.pages.experiment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import com.heinrichreimersoftware.materialintro.app.SlideFragment
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.ANDROID_ASSET_PATH
import us.feras.mdv.MarkdownView

/**
 * Created by Young-Ho on 1/26/2017.
 */
class ConsentFormSlideFragment : SlideFragment() {
    private lateinit var markdownView: MarkdownView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.slide_consent_form, container, false)
        markdownView = view.findViewById(R.id.ui_markdown) as MarkdownView

        markdownView.loadMarkdownFile(
                "$ANDROID_ASSET_PATH/consent/${resources.getString(R.string.informed_consent_filename)}",
                "$ANDROID_ASSET_PATH/consent/style.css"
        )

        markdownView.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
        markdownView.setScrollbarFadingEnabled(false)

        return view
    }

    override fun canGoBackward(): Boolean {
        return false
    }
}