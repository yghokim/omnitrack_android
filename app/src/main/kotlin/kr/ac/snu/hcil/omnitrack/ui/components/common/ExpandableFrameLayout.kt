package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.FrameLayout
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-07-28.
 */
class ExpandableFrameLayout : FrameLayout {

    var isCollapsed: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                if (value == true) {
                    println("collapse")
                    collapse()
                } else {
                    println("expand")
                    expand()
                }
            }
        }

    private lateinit var collapsedView: ViewGroup
    private lateinit var expandedView: ViewGroup

    private var collapseButton: View? = null
    private var expandButton: View? = null

    private var collapsedViewId: Int = 0
    private var expandedViewId: Int = 0
    private var collapseButtonId: Int = 0
    private var expandButtonId: Int = 0
    private var attributeIsCollapsed: Boolean = true


    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.ExpandableFrameLayout, 0, 0)
        try {
            collapsedViewId = a.getResourceId(R.styleable.ExpandableFrameLayout_ex_collapsedContainerId, R.id.ui_collapsed_view)
            expandedViewId = a.getResourceId(R.styleable.ExpandableFrameLayout_ex_expandedContainerId, R.id.ui_expanded_view)
            collapseButtonId = a.getResourceId(R.styleable.ExpandableFrameLayout_ex_collapseButtonId, 0)
            expandButtonId = a.getResourceId(R.styleable.ExpandableFrameLayout_ex_collapseButtonId, 0)

            attributeIsCollapsed = a.getBoolean(R.styleable.ExpandableFrameLayout_ex_collapsed, true)
        } finally {
            a.recycle()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        collapsedView = findViewById(collapsedViewId) as ViewGroup
        expandedView = findViewById(expandedViewId) as ViewGroup
        if (collapseButtonId != 0) {
            collapseButton = findViewById(collapseButtonId)
            collapseButton?.setOnClickListener { view ->
                collapse()
            }
        }

        if (expandButtonId != 0) {
            expandButton = findViewById(expandButtonId)
            expandButton?.setOnClickListener { view ->
                expand()
            }
        }

        isCollapsed = attributeIsCollapsed

    }

    fun expand() {
        collapsedView.visibility = View.GONE
        expandedView.visibility = View.VISIBLE
    }

    fun collapse() {
        collapsedView.visibility = View.VISIBLE
        expandedView.visibility = View.GONE
    }


    inner class ResizeAnim(private val view: View, private val initialHeight: Int, private val targetHeight: Int, private val down: Boolean) : Animation() {

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            val newHeight: Int
            if (down) {
                newHeight = ((targetHeight - initialHeight) * interpolatedTime + initialHeight).toInt()
            } else {
                newHeight = ((targetHeight - initialHeight) * (1 - interpolatedTime) + initialHeight).toInt()
            }
            view.layoutParams.height = newHeight
            view.requestLayout()
        }

        override fun initialize(width: Int, height: Int, parentWidth: Int,
                                parentHeight: Int) {
            super.initialize(width, height, parentWidth, parentHeight)
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }

}