package kr.ac.snu.hcil.omnitrack.ui.components.common.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.auth.AuthConstants
import okhttp3.HttpUrl

open class WebServiceLoginActivity : AppCompatActivity(), View.OnClickListener {

    companion object {

        const val EXTRA_RESQUEST_URL = "requestUrl"
        const val EXTRA_SERVICE_NAME = "serviceName"

        fun makeIntent(url: String, serviceName: String, context: Context, activityClass: Class<out WebServiceLoginActivity> = WebServiceLoginActivity::class.java): Intent {
            val intent = Intent(context, activityClass)
            intent.putExtra(EXTRA_RESQUEST_URL, url)
            intent.putExtra(EXTRA_SERVICE_NAME, serviceName)
            return intent
        }
    }

    private lateinit var webView: WebView
    private lateinit var titleView: TextView
    private lateinit var cancelButton: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.auth_webview_screen)

        webView = findViewById(R.id.webView) as WebView

        titleView = findViewById(R.id.title) as TextView
        cancelButton = findViewById(R.id.ui_button_cancel)
        cancelButton.setOnClickListener(this)

        webView.settings.javaScriptEnabled = true
        webView.setWebViewClient(
                object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        onPageFinished(url)
                    }

                }
        )
    }

    override fun onStart() {
        super.onStart()

        if (intent.hasExtra(EXTRA_RESQUEST_URL)) {
            val url = intent.getStringExtra(EXTRA_RESQUEST_URL)
            val serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME)
            titleView.text = String.format(resources.getString(R.string.msg_format_login_to), serviceName)

            webView.loadUrl(url)
        }
    }


    open fun onPageFinished(url: String) {
        println("webView finished - $url")
        finishIfPossible(url)
    }

    open fun finishIfPossible(redirectedUrl: String) {
        val parsedUrl = HttpUrl.parse(redirectedUrl)
        val code = parsedUrl.queryParameter(AuthConstants.PARAM_CODE)
        if (!code.isNullOrBlank()) {
            val result = Intent()
            result.putExtra(AuthConstants.PARAM_CODE, code)
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    override fun onClick(view: View) {
        if (view === cancelButton) {
            setResult(RESULT_CANCELED)
            finish()
        }
    }
}
