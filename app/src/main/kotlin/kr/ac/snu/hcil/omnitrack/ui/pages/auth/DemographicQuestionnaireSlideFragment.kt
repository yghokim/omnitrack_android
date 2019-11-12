package kr.ac.snu.hcil.omnitrack.ui.pages.auth

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.os.bundleOf
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.slide_demographic.view.*
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.android.common.getStringCompat

class DemographicQuestionnaireSlideFragment : SignUpActivity.SlideFragment(SignUpActivity.ESlide.DEMOGRAPHIC_QUESTIONNAIRE) {

    companion object {
        const val INITIALIZE_URL = "file:///android_asset/web/questionnaire/index.html"

        fun getInstance(schema: String): DemographicQuestionnaireSlideFragment {
            return DemographicQuestionnaireSlideFragment().apply {
                arguments = bundleOf("questionnaireSchema" to schema)
            }
        }
    }

    private val client = QuestionnaireWebViewClient()
    private lateinit var schemaString: String

    private var webView: WebView? = null


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.slide_demographic, container, false)
        val webView = view.ui_webview
        this.webView = webView

        webView.settings.javaScriptEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.settings.setSupportZoom(false)
        webView.settings.allowFileAccess = true
        webView.isVerticalFadingEdgeEnabled = true
        webView.isVerticalScrollBarEnabled = true
        webView.isHorizontalScrollBarEnabled = false
        webView.addJavascriptInterface(this, "ExternalHandler")
        webView.webViewClient = client
        webView.loadUrl(INITIALIZE_URL)

        arguments?.getString("questionnaireSchema")?.let { schemaString ->
            this.schemaString = schemaString
        }

        return view
    }

    @JavascriptInterface
    fun onSubmitted(serializedError: String?, serializedValues: String?) {
        println("serializedError: $serializedError")
        println("serializedValues: $serializedValues")
        if (serializedError != null) {
            val errorObj = (requireContext().applicationContext as OTAndroidApp).applicationComponent.genericGson().fromJson(serializedError, JsonObject::class.java)
            if (errorObj.getStringCompat("error") == "RequiredFields") {
                Toast.makeText(requireContext(), "Fill up all the required questions.", Toast.LENGTH_LONG).show()
            }
        } else {
            val valueObj = (requireContext().applicationContext as OTAndroidApp).applicationComponent.genericGson().fromJson(serializedValues, JsonObject::class.java)
            viewModel.demographicAnswers = valueObj
            viewModel.goNext(slide)
        }
    }

    override fun onNextTried() {
        webView?.evaluateJavascript("submit()") { result ->
            println(result)
        }
    }

    inner class QuestionnaireWebViewClient : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (url?.startsWith(INITIALIZE_URL) == true) {
                view?.evaluateJavascript("init($schemaString)") { result ->
                    println("initResult:")
                    println(result)
                }
            }
        }
    }
}