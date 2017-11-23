package kr.ac.snu.hcil.omnitrack.ui.components.visualization

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import io.reactivex.disposables.SerialDisposable
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import org.jetbrains.anko.runOnUiThread
import kotlin.properties.Delegates

/**
 * Created by younghokim on 2017. 11. 23..
 */
class WebBasedChartView : WebView, IChartView {

    override var model: ChartModel<*>? by Delegates.observable(null as ChartModel<*>?)
    { prop, old, new ->
        if (old !== new) {
            if (old != null) {
                modelSubscription.set(null)
                old.recycle()
            }

            if (new != null) {

                //TODO update chartClient Data
            }
        }
    }
    private var intrinsicHeight: Int = -1

    private val modelSubscription = SerialDisposable()
    private val client = ChartWebViewClient()

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
        settings.javaScriptEnabled = true
        addJavascriptInterface(this, "ChartView")
        webViewClient = client
    }

    @JavascriptInterface
    fun setIntrinsicChartHeight(height: Float) {
        context.runOnUiThread {
            intrinsicHeight = (height + 0.5f).toInt()
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        if (intrinsicHeight != -1 && model != null) {
            val widthMode = MeasureSpec.getMode(widthMeasureSpec)
            val widthSize = MeasureSpec.getSize(widthMeasureSpec)
            val heightMode = MeasureSpec.getMode(heightMeasureSpec)
            val heightSize = MeasureSpec.getSize(heightMeasureSpec)

            val measuredWidth: Int
            val measuredHeight: Int

            if (widthMode == MeasureSpec.EXACTLY) {
                measuredWidth = widthSize
            } else if (widthMode == MeasureSpec.AT_MOST) {
                measuredWidth = widthSize
            } else {
                measuredWidth = 400 + paddingLeft + paddingRight
            }

            if (heightMode == MeasureSpec.EXACTLY) {
                measuredHeight = heightSize
            } else if (heightMode == MeasureSpec.AT_MOST) {
                measuredHeight = Math.min(intrinsicHeight + paddingTop + paddingBottom, heightSize)
            } else {
                measuredHeight = intrinsicHeight + paddingTop + paddingBottom
            }

            setMeasuredDimension(measuredWidth, measuredHeight)
        } else super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun setWebViewClient(client: WebViewClient?) {
        if (this.client != client) {
            throw IllegalAccessException("don't use setWebViewClient on WebBasedChartView.")
        }
    }


    private fun subscribeToModelEvent(model: ChartModel<*>) {
        modelSubscription.set(
                model.stateObservable.subscribe { state ->
                    when (state) {
                        ChartModel.State.Loaded -> {
                            //TODO model data changed
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

    private inner class ChartWebViewClient : WebViewClient()
}