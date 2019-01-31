package kr.ac.snu.hcil.omnitrack.utils

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-07-25
 */
object InterfaceHelper {

    val grayscaleColorFilter: ColorFilter by lazy {
        ColorMatrixColorFilter(ColorMatrix().apply { this.setSaturation(0f) })
    }

    fun removeButtonTextDecoration(button: Button) {
        button.transformationMethod = null
        button.isAllCaps = false
    }

    @Suppress("DEPRECATION")
    fun setTextAppearance(tv: TextView, style: Int) {
        if (android.os.Build.VERSION.SDK_INT < 23) {
            tv.setTextAppearance(tv.context, style)
        } else {
            tv.setTextAppearance(style)
        }

    }

    @Suppress("DEPRECATION")
    fun setTextAppearance(tv: Button, style: Int) {
        if (android.os.Build.VERSION.SDK_INT < 23) {
            tv.setTextAppearance(tv.context, style)
        } else {
            tv.setTextAppearance(style)
        }

    }

    fun alertBackground(views: Array<View>, alertColorRes: Int = R.color.colorRed_Light, startAlpha: Float = 0.5f, duration: Long = 1200): ValueAnimator {
        val color = ContextCompat.getColor(views.first().context, alertColorRes)

        val colorAnimator = ValueAnimator.ofFloat(startAlpha, 0f)
        colorAnimator.duration = duration
        colorAnimator.addUpdateListener {
            animator ->
            for (view in views)
                view.setBackgroundColor(ColorUtils.setAlphaComponent(color, (255 * animator.animatedValue as Float).toInt()))
        }
        colorAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(p0: Animator?) {
            }

            override fun onAnimationEnd(p0: Animator?) {

                for (view in views)
                    view.background = null
            }

            override fun onAnimationCancel(p0: Animator?) {

                for (view in views)
                    view.background = null
            }

            override fun onAnimationStart(p0: Animator?) {

                for (view in views)
                    view.setBackgroundColor(color)
            }

        })
        return colorAnimator
    }

    fun alertBackground(view: View, alertColorRes: Int = R.color.colorRed_Light, startAlpha: Float = 0.5f, duration: Long = 1200): ValueAnimator {
        val color = ContextCompat.getColor(view.context, alertColorRes)

        val colorAnimator = ValueAnimator.ofFloat(startAlpha, 0f)
        colorAnimator.duration = duration
        colorAnimator.addUpdateListener {
            animator ->
            view.setBackgroundColor(ColorUtils.setAlphaComponent(color, (255 * animator.animatedValue as Float).toInt()))
        }
        colorAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(p0: Animator?) {
            }

            override fun onAnimationEnd(p0: Animator?) {
                view.background = null
            }

            override fun onAnimationCancel(p0: Animator?) {
                view.background = null
            }

            override fun onAnimationStart(p0: Animator?) {
                view.setBackgroundColor(color)
            }

        })
        return colorAnimator
    }

    fun setViewGrayscale(view: View, grayscale: Boolean) {
        setViewColorFilter(view, grayscale, grayscaleColorFilter, null)
    }

    fun setViewColorFilter(view: View, filtered: Boolean, filter: ColorFilter, toggledAlpha: Float?) {
        if (filtered) {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, Paint().apply { this.colorFilter = filter })
            if (toggledAlpha != null) {
                view.alpha = toggledAlpha
            }
        } else {
            view.setLayerType(View.LAYER_TYPE_NONE, null)
            if (toggledAlpha != null) {
                view.alpha = 1.0f
            }
        }
    }
}