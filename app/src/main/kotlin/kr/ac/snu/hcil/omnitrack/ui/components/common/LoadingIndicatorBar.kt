package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import com.github.ybq.android.spinkit.SpinKitView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.inflateContent

/**
 * Created by younghokim on 2017. 3. 8..
 */
class LoadingIndicatorBar : FrameLayout {

    private val progressBar: SpinKitView
    private val messageView: TextView

    private val showAnimator: ValueAnimator
    private val dismissAnimator: ValueAnimator
    private val animationUpdateListener: ValueAnimator.AnimatorUpdateListener


    private var isIndicatorShown: Boolean = false
        private set(value)
        {
            if(value!=field) {
                /*
                if (value == true) {
                    this.layoutParams?.height = expandedHeight
                    this.visibility = View.VISIBLE
                } else {
                    this.layoutParams?.height = 0
                    this.visibility = View.GONE
                }*/

                field = value
            }
        }

    private var message: CharSequence get()=messageView.text.toString()
        set(value){
            messageView.text = value
        }

    private val expandedHeight: Int by lazy{
        context.resources.getDimensionPixelSize(R.dimen.loading_indicator_bar_height)
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        inflateContent(R.layout.component_loading_indicator_bar, true)
        progressBar = findViewById(R.id.ui_progress_bar) as SpinKitView
        messageView = findViewById(R.id.ui_message) as TextView

        animationUpdateListener = ValueAnimator.AnimatorUpdateListener {
            animator->
            val heightValue = animator.animatedValue as Float
            this@LoadingIndicatorBar.layoutParams?.height = (heightValue+.5f).toInt()
            this@LoadingIndicatorBar.requestLayout()
        }

        showAnimator = ValueAnimator.ofFloat(0f, expandedHeight.toFloat()).apply{
            duration = 250
            interpolator = DecelerateInterpolator()

            addUpdateListener(animationUpdateListener)
            addListener(object: Animator.AnimatorListener {
                override fun onAnimationEnd(p0: Animator?) {
                }

                override fun onAnimationCancel(p0: Animator?) {
                    this@LoadingIndicatorBar.visibility = View.INVISIBLE
                }

                override fun onAnimationStart(p0: Animator?) {
                    this@LoadingIndicatorBar.visibility = View.VISIBLE
                    this@LoadingIndicatorBar.layoutParams?.height = 0
                }

                override fun onAnimationRepeat(p0: Animator?) {

                }

            })
        }

        dismissAnimator = ValueAnimator.ofFloat(expandedHeight.toFloat(), 0f).apply{
            duration = 400
            interpolator = DecelerateInterpolator()

            addUpdateListener(animationUpdateListener)
            addListener(object: Animator.AnimatorListener {
                override fun onAnimationEnd(p0: Animator?) {
                    this@LoadingIndicatorBar.visibility = View.INVISIBLE
                    this@LoadingIndicatorBar.layoutParams?.height = 0
                }

                override fun onAnimationCancel(p0: Animator?) {
                    this@LoadingIndicatorBar.visibility = View.VISIBLE
                    this@LoadingIndicatorBar.layoutParams?.height = expandedHeight
                }

                override fun onAnimationStart(p0: Animator?) {
                    this@LoadingIndicatorBar.layoutParams?.height = expandedHeight
                    isIndicatorShown = false
                }

                override fun onAnimationRepeat(p0: Animator?) {

                }

            })
        }
    }

    fun setMessage(res: Int){
        this.messageView.setText(res)
    }

    fun show(){
        if(!isIndicatorShown) {
            if (dismissAnimator.isRunning) {
                dismissAnimator.reverse()
            } else showAnimator.start()

            isIndicatorShown = true
        }
    }

    fun dismiss(){
        if(isIndicatorShown) {
            if (showAnimator.isRunning) {
                showAnimator.reverse()
            } else dismissAnimator.start()

            isIndicatorShown = false
        }
    }

}