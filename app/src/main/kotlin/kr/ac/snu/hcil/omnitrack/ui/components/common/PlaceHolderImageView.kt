package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import com.github.ybq.android.spinkit.SpinKitView
import com.github.ybq.android.spinkit.style.Circle
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by younghokim on 2017. 3. 12..
 */
class PlaceHolderImageView : FrameLayout {

    enum class Mode { LOADING, ERROR, IMAGE, EMPTY }

    var currentMode: Mode = Mode.LOADING
        set(value) {
            if (field != value) {
                when (value) {
                    Mode.LOADING -> {
                        imageView.setImageResource(0)
                        loadingIndicator.visibility = View.VISIBLE
                    }
                    Mode.ERROR -> {
                        imageView.setImageResource(0)
                    }
                    Mode.IMAGE -> {
                        loadingIndicator.visibility = View.GONE
                    }
                    Mode.EMPTY -> {
                        loadingIndicator.visibility = View.GONE
                        imageView.setImageResource(0)
                    }
                }

                field = value
            }
        }

    private val loadingIndicator: ProgressBar
    val imageView: ImageView

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        setBackgroundColor(ContextCompat.getColor(context, R.color.editTextFormBackground))

        val padding = (8 * context.resources.displayMetrics.density + .5f).toInt()
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
        val indicatorMargin = (8 * context.resources.displayMetrics.density + .5f).toInt()
        lp.setMargins(indicatorMargin, indicatorMargin, indicatorMargin, indicatorMargin)
        loadingIndicator.layoutParams = lp

        addView(loadingIndicator)

    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        super.setLayoutParams(params)
        imageView.layoutParams = FrameLayout.LayoutParams(params)
    }

}