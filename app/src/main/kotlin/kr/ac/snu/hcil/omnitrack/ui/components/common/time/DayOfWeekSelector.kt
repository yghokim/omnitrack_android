package kr.ac.snu.hcil.omnitrack.ui.components.common.time

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatButton
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.BitwiseOperationHelper
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.dipSize
import kr.ac.snu.hcil.omnitrack.utils.events.Event

/**
 * Created by Young-Ho on 8/25/2016.
 */
class DayOfWeekSelector : LinearLayout, View.OnClickListener {

    var allowNoneSelection: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                checkNonSelection()
            }
        }

    private val checkedFlags = BooleanArray(7) { false }

    val selectionFlagsChanged = Event<Int>()

    var checkedFlagsInteger: Int get() {

        var flags = 0
        for (flag in checkedFlags.withIndex()) {
            flags = flags or ((if (flag.value) 1 else 0) shl (6 - flag.index))
        }

        println("checked day of week flags: $flags")

        return flags
    }
        set(value) {
            val original = checkedFlagsInteger
            var changed = false
            for (day in 0..6) {
                if (setChecked(day, BitwiseOperationHelper.getBooleanAt(value, 6 - day))) {
                    changed = true
                }
            }

            if (changed) {
                checkNonSelection()
                val new = checkedFlagsInteger
                if (original != new) {
                    selectionFlagsChanged.invoke(this, new)
                }
            }
        }


    private val buttons: Array<CircleBackgroundButton>

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    init {
        orientation = HORIZONTAL

        val oneLetterDayNames = context.resources.getStringArray(R.array.days_of_week_oneletter)


        buttons = Array(7) {
            index ->
            val button = CircleBackgroundButton(context)

            val lp = LayoutParams(0, resources.getDimensionPixelSize(R.dimen.button_height_normal))
            lp.weight = 1f
            lp.gravity = Gravity.CENTER_VERTICAL
            button.inset = dipSize(context, 3)
            button.tag = index
            button.text = oneLetterDayNames[index].toUpperCase()
            button.setPadding(0, 0, 0, 0)
            button.setOnClickListener(this)
            button.layoutParams = lp
            button.includeFontPadding = false
            button.setBackgroundColor(Color.TRANSPARENT)
            InterfaceHelper.setTextAppearance(button, R.style.dayOfWeekSelectorLetterAppearance_Unselected)
            addView(button)
            button
        }
    }

    override fun onClick(view: View) {
        if (view.tag is Int && view is CircleBackgroundButton) {
            val day = view.tag as Int

            this[day] = !checkedFlags[day]
        }
    }

    operator fun get(day: Int): Boolean {
        return checkedFlags[day]
    }

    operator fun set(day: Int, value: Boolean) {

        val lastFlags = checkedFlagsInteger
        val checkedChanged = setChecked(day, value)

        if (checkedChanged) {
            checkNonSelection()

            val newFlags = checkedFlagsInteger
            if (lastFlags != newFlags) {
                selectionFlagsChanged.invoke(this, newFlags)
            }
        }
    }

    private fun setChecked(day: Int, checked: Boolean): Boolean {
        if (checkedFlags[day] != checked) {
            checkedFlags[day] = checked
            val button = buttons[day]

            if (checked) {

                button.drawCircle = true
                InterfaceHelper.setTextAppearance(button, R.style.dayOfWeekSelectorLetterAppearance_Selected)
            } else {
                button.drawCircle = false
                InterfaceHelper.setTextAppearance(button, R.style.dayOfWeekSelectorLetterAppearance_Unselected)
            }

            return true
        } else return false
    }

    private fun checkNonSelection(): Boolean {
        if (!allowNoneSelection) {
            if (checkedFlags.indexOf(true) == -1) {
                for (day in 0..6) {
                    setChecked(day, true)
                }
                return true
            } else return false
        } else return false
    }

    class CircleBackgroundButton : AppCompatButton, ValueAnimator.AnimatorUpdateListener {

        var inset: Float = 0f

        private var centerX: Float = 0f
        private var centerY: Float = 0f
        private var radius: Float = 0f

        private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        private val animator: ValueAnimator

        var drawCircle: Boolean = false
            set(value) {
                if (field != value) {
                    field = value

                    if (animator.isRunning) {
                        animator.cancel()
                        animator.currentPlayTime = animator.duration - animator.currentPlayTime
                    }

                    animator.start()
                }
            }

        constructor(context: Context?) : super(context)
        constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
        constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

        init {
            circlePaint.style = Paint.Style.FILL
            circlePaint.color = ContextCompat.getColor(context, R.color.textColorMid)

            animator = ValueAnimator.ofFloat(0f, 1f)
            animator.duration = 300
            animator.interpolator = DecelerateInterpolator()
            animator.currentPlayTime = animator.duration
            animator.addUpdateListener(this)
        }


        override fun onAnimationUpdate(p0: ValueAnimator) {
            invalidate()
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(changed, left, top, right, bottom)
            if (changed) {
                val contentWidth = right - left - paddingLeft - paddingRight
                val contentHeight = bottom - top - paddingTop - paddingBottom
                radius = Math.min(contentWidth, contentHeight) * .5f
                centerX = paddingLeft + contentWidth * .5f
                centerY = paddingTop + contentHeight * .5f
            }
        }

        override fun onDraw(canvas: Canvas) {
            if (radius - inset > 0) {
                canvas.drawCircle(centerX, centerY, (radius - inset) * (if (drawCircle) {
                    animator.animatedValue as Float
                } else {
                    1 - animator.animatedValue as Float
                }), circlePaint)
            }

            super.onDraw(canvas)
        }
    }
}