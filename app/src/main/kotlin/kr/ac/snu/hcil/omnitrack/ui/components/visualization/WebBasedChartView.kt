package kr.ac.snu.hcil.omnitrack.ui.components.visualization

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import com.github.ybq.android.spinkit.SpinKitView
import io.reactivex.disposables.SerialDisposable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.IWebBasedChartModel
import kr.ac.snu.hcil.omnitrack.utils.argbIntToCssString
import org.jetbrains.anko.runOnUiThread
import kotlin.properties.Delegates

/**
 * Created by younghokim on 2017. 11. 23..
 */
class WebBasedChartView : FrameLayout, IChartView {

    companion object {
        const val INITIALIZE_URL = "file:///android_asset/web/visualization/chart.html"
    }

    override var model: ChartModel<*>? by Delegates.observable(null as ChartModel<*>?)
    { prop, old, new ->
        if (old !== new) {
            if (old != null) {
                modelSubscription.set(null)
                old.recycle()
            }

            if (new != null) {
                subscribeToModelEvent(new)
            }
        }
    }

    private val castedModel: IWebBasedChartModel? get() = model as IWebBasedChartModel

    private var intrinsicRatio: Float = 0f

    private val modelSubscription = SerialDisposable()

    private lateinit var webView: WebView
    private lateinit var spinner: ProgressBar

    private val client = ChartWebViewClient()

    private var wasDataChangedPending: Boolean = false
    private var isWebViewReady: Boolean = false

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun init(context: Context, attrs: AttributeSet?) {
        webView = WebView(context, attrs)
        webView.id = View.generateViewId()
        addView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.settings.setSupportZoom(false)
        webView.settings.allowFileAccess = true
        webView.isVerticalFadingEdgeEnabled = false
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.addJavascriptInterface(this, "chartView")
        webView.webViewClient = client
        webView.loadUrl(INITIALIZE_URL)

        spinner = SpinKitView(context, attrs).apply {
            this.setIndeterminateDrawable(com.github.ybq.android.spinkit.style.Circle())
            this.setColor(ContextCompat.getColor(context, R.color.colorPointed))
        }
        spinner.id = View.generateViewId()
        val spinnerLayoutParams = FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        spinnerLayoutParams.topMargin = context.resources.getDimensionPixelSize(R.dimen.activity_vertical_margin)
        spinnerLayoutParams.bottomMargin = context.resources.getDimensionPixelSize(R.dimen.activity_vertical_margin)
        spinnerLayoutParams.gravity = Gravity.CENTER
        spinner.layoutParams = spinnerLayoutParams
        //spinner.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPointed))
        addView(spinner)
    }

    @JavascriptInterface
    fun getChartType(): String {
        return castedModel?.getChartTypeCommand() ?: ""
    }

    @JavascriptInterface
    fun requestChartDataString(): String {
        println("chartmodel data was requested by javascript.")
        return castedModel?.getDataInJsonString() ?: "{}"
    }

    @JavascriptInterface
    fun onChartCanvasResized(width: Int, height: Int) {
        intrinsicRatio = width.toFloat() / height
        context.runOnUiThread {
            webView.layoutParams = FrameLayout.LayoutParams(width, height)
            requestLayout()
        }
    }

    @JavascriptInterface
    fun requestGlobalOptions(): String {
        return "{\"elementMainColor\":\"${argbIntToCssString(ContextCompat.getColor(this.context, R.color.colorPointed))}\"}"
    }

    private fun subscribeToModelEvent(model: ChartModel<*>) {
        modelSubscription.set(
                model.stateObservable.subscribe { state ->
                    when (state) {
                        ChartModel.State.Loaded -> {
                            if (isWebViewReady) {
                                webView.loadUrl("javascript:onDataChanged()")
                            } else {
                                println("webview is not ready to draw chart. store pending flag.")
                                wasDataChangedPending = true
                            }
                        }
                    }
                }
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        model?.let {
            subscribeToModelEvent(it)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        modelSubscription.set(null)
    }

    inner class ChartWebViewClient : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            println("webview page finished for $url")
            if (!isWebViewReady && url.startsWith(INITIALIZE_URL)) {
                println("webview chart is now ready.")
                spinner.visibility = View.GONE
                isWebViewReady = true
            }

            if (wasDataChangedPending) {
                println("there is a pending update of data for webview. call Javascript function.")
                wasDataChangedPending = false
                webView.loadUrl("javascript:onDataChanged()")
            }
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            super.onLoadResource(view, url)
            println("onLoadResource")
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            println("webview page started: $url")
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            super.onReceivedError(view, request, error)
            println("webview error when $request")
            println(error)
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            super.onReceivedSslError(view, handler, error)
            println("received SSL error")
            println(handler.obtainMessage())
            println(error.primaryError)
        }

        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
            super.onReceivedHttpError(view, request, errorResponse)
            println("http error when $request")
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            println("render process was gone.")
            return super.onRenderProcessGone(view, detail)
        }
    }
}