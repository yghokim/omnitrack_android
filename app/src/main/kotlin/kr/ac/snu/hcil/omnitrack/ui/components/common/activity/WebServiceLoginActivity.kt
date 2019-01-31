package kr.ac.snu.hcil.omnitrack.ui.components.common.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import butterknife.bindView
import com.github.ybq.android.spinkit.SpinKitView
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.auth.AuthConstants
import okhttp3.HttpUrl

open class WebServiceLoginActivity : AppCompatActivity(), View.OnClickListener {

    companion object {

        const val EXTRA_RESQUEST_URL = "requestUrl"
        const val EXTRA_SERVICE_NAME = "serviceName"
        const val EXTRA_TITLE_OVERRIDE = "overrideTitle"
        const val EXTRA_RETURNED_PARAMETERS = "returnedParameters"

        fun makeIntent(url: String, serviceName: String, overrideTitle: String?, context: Context, activityClass: Class<out WebServiceLoginActivity> = WebServiceLoginActivity::class.java): Intent {
            val intent = Intent(context, activityClass)
            intent.putExtra(EXTRA_RESQUEST_URL, url)
            intent.putExtra(EXTRA_SERVICE_NAME, serviceName)
            overrideTitle?.let {
                intent.putExtra(EXTRA_TITLE_OVERRIDE, it)
            }
            return intent
        }
    }

    private val webView: WebView by bindView(R.id.webView)
    private val titleView: TextView by bindView(R.id.title)
    private val cancelButton: View by bindView(R.id.ui_button_cancel)
    private val loadingIndicator: SpinKitView by bindView(R.id.ui_loading_indicator)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.auth_webview_screen)

        cancelButton.setOnClickListener(this)

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                loadingIndicator.visibility = View.INVISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                onPageFinished(url)
                loadingIndicator.visibility = View.INVISIBLE
            }

        }

        if (intent.hasExtra(EXTRA_RESQUEST_URL)) {
            val url = intent.getStringExtra(EXTRA_RESQUEST_URL)
            val serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME)
            titleView.text = intent.getStringExtra(EXTRA_TITLE_OVERRIDE) ?: getTitleText(serviceName)
            loadingIndicator.visibility = View.VISIBLE
            webView.loadUrl(url)
        }
    }

    open fun getTitleText(serviceName: String): String {
        return String.format(resources.getString(R.string.msg_format_login_to), serviceName)
    }

    open fun onPageFinished(url: String) {
        finishIfPossible(url)
    }

    open fun finishIfPossible(redirectedUrl: String) {
        val parsedUrl = HttpUrl.parse(redirectedUrl)
        if (parsedUrl != null) {
            val code = parsedUrl.queryParameter(AuthConstants.PARAM_CODE)
            if (!code.isNullOrBlank()) {
                val result = Intent()
                result.putExtra(AuthConstants.PARAM_CODE, code)

                val paramNames = parsedUrl.queryParameterNames()
                if (paramNames.size >= 2) {
                    val returnedParameters = Bundle()
                    paramNames.forEach { param ->
                        if (param != AuthConstants.PARAM_CODE) {
                            returnedParameters.putString("returned::" + param, parsedUrl.queryParameter(param))
                        }
                    }
                    result.putExtra(EXTRA_RETURNED_PARAMETERS, returnedParameters)
                }

                setResult(Activity.RESULT_OK, result)
                finish()
            }
        }
    }

    override fun onClick(view: View) {
        if (view === cancelButton) {
            tryCancel()
        }
    }

    override fun onBackPressed() {
        tryCancel()
    }

    protected open fun tryCancel() {
        DialogHelper.makeYesNoDialogBuilder(this, BuildConfig.APP_NAME, resources.getString(R.string.msg_confirm_cancel_and_close_process), R.string.msg_close, R.string.msg_cancel, {
            setResult(RESULT_CANCELED)
            finish()
        }, null).show()
    }
}
