package kr.ac.snu.hcil.android.common.view.image

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.github.ybq.android.spinkit.SpinKitView
import com.github.ybq.android.spinkit.style.Circle
import kr.ac.snu.hcil.android.common.dipSize
import kr.ac.snu.hcil.android.common.view.R
import kr.ac.snu.hcil.android.common.view.applyTint
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

/**
 * Created by younghokim on 2017. 3. 12..
 */
open class PlaceHolderImageView : FrameLayout {

    companion object {
        private val tooltipIdSeed = AtomicInteger()

        protected fun makeNewTooltipId(): Int {
            return tooltipIdSeed.addAndGet(1)
        }
    }

    enum class Mode { LOADING, ERROR, IMAGE, EMPTY }

    var currentMode: Mode = Mode.LOADING
        set(value) {
            if (field != value) {
                when (value) {
                    Mode.LOADING -> {
                        imageView.setImageResource(0)
                        loadingIndicator.visibility = View.VISIBLE
                        errorRetryButton?.visibility = View.GONE
                        this.minimumHeight = 0
                        this.setBackgroundColor(normalBackgroundColor)
                    }
                    Mode.ERROR -> {
                        imageView.setImageResource(0)
                        loadingIndicator.visibility = View.GONE
                        if (!makeErrorButtonIfNecessary()) {
                            errorRetryButton?.visibility = VISIBLE
                        }

                        errorRetryButton?.text = null

                        this.minimumHeight = 0
                        this.setBackgroundColor(normalBackgroundColor)
                    }
                    Mode.IMAGE -> {
                        loadingIndicator.visibility = View.GONE
                        errorRetryButton?.visibility = View.GONE
                        this.minimumHeight = 0
                        this.setBackgroundColor(normalBackgroundColor)
                    }
                    Mode.EMPTY -> {
                        loadingIndicator.visibility = View.GONE
                        errorRetryButton?.visibility = View.GONE
                        imageView.setImageResource(0)
                        this.minimumHeight = dipSize(context, 40).roundToInt()
                        this.background = emptyBackground
                    }
                }

                field = value
            }
        }

    private val emptyBackground: Drawable by lazy {
        applyTint(ContextCompat.getDrawable(context, R.drawable.hatching_repeated_big)!!, Color.parseColor("#e0e0e0"))
    }

    private val normalBackgroundColor: Int by lazy {
        ContextCompat.getColor(context, R.color.editTextFormBackground)
    }

    private val loadingIndicator: SpinKitView
    val imageView: ImageView
    var errorRetryButton: AppCompatButton? = null

    var onRetryHandler: (() -> Unit)? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        setBackgroundColor(ContextCompat.getColor(context, R.color.editTextFormBackground))

        val padding = dipSize(context, 8).roundToInt()
        setPadding(padding, padding, padding, padding)

        imageView = ImageView(context)
        imageView.adjustViewBounds = true
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        addView(imageView)

        loadingIndicator = SpinKitView(context)
        loadingIndicator.setColor(ContextCompat.getColor(context, R.color.colorPointed))
        loadingIndicator.isIndeterminate = true
        loadingIndicator.setIndeterminateDrawable(Circle())

        val lp = FrameLayout.LayoutParams(
                context.resources.getDimensionPixelSize(R.dimen.image_placeholder_loading_indicator_size),
                context.resources.getDimensionPixelSize(R.dimen.image_placeholder_loading_indicator_size))
        lp.gravity = Gravity.CENTER
        val indicatorMargin = dipSize(context, 8).roundToInt()
        lp.setMargins(indicatorMargin, indicatorMargin, indicatorMargin, indicatorMargin)
        loadingIndicator.layoutParams = lp

        addView(loadingIndicator)

    }

    private fun makeErrorButtonIfNecessary(): Boolean {
        if (errorRetryButton == null) {
            val button = AppCompatButton(context)

            button.setSupportAllCaps(false)
            button.setTextColor(ContextCompat.getColor(context, R.color.colorRed_Light))
            button.compoundDrawablePadding = dipSize(context, 8).roundToInt()
            button.background = ContextCompat.getDrawable(context, R.drawable.transparent_button_background)

            button.setCompoundDrawablesRelativeWithIntrinsicBounds(applyTint(ContextCompat.getDrawable(context, R.drawable.error_dark)!!, ContextCompat.getColor(context, R.color.colorRed)), null, null, null)
            val lp = FrameLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    context.resources.getDimensionPixelSize(R.dimen.button_height_small))
            lp.gravity = Gravity.CENTER

            button.layoutParams = lp

            button.setOnClickListener { onRetryHandler?.invoke() }

            errorRetryButton = button
            addView(button)

            return true
        } else return false
    }

    fun setErrorMode(tooltipMessage: String? = null) {
        currentMode = Mode.ERROR
        if (tooltipMessage != null) {
            errorRetryButton?.text = tooltipMessage
            /*
            errorTooltip?.setText(tooltipMessage)
            errorTooltip?.show()*/

        }
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams) {
        super.setLayoutParams(params)
        imageView.layoutParams = FrameLayout.LayoutParams(params)
    }

}