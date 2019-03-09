package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.annotation.TargetApi
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.PluralsRes
import androidx.constraintlayout.widget.ConstraintLayout
import kr.ac.snu.hcil.android.common.events.Event
import kr.ac.snu.hcil.android.common.view.inflateContent
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-07-25.
 */
class NumericUpDown : ConstraintLayout, INumericUpDown {
    override var minValue: Int
        get() = impl.minValue
        set(value) {
            impl.minValue = value
        }
    override var maxValue: Int
        get() = impl.maxValue
        set(value) {
            impl.maxValue = value
        }
    override val value: Int
        get() = impl.value
    override var displayedValues: Array<String>?
        get() = impl.displayedValues
        set(value) {
            impl.displayedValues = value
        }
    override var formatter: ((Int) -> String)?
        get() = impl.formatter
        set(value) {
            impl.formatter = value
        }
    override var quantityResId: Int?
        get() = impl.quantityResId
        set(@PluralsRes value) {
            impl.quantityResId = value
        }
    override var zeroPad: Int
        get() = impl.zeroPad
        set(value) {
            impl.zeroPad = value
        }
    override val valueChanged: Event<INumericUpDown.ChangeArgs>
        get() = impl.valueChanged
    override var allowLongPress: Boolean
        get() = impl.allowLongPress
        set(value) {
            impl.allowLongPress = value
        }

    override fun setValue(newValue: Int, changeType: INumericUpDown.ChangeType, delta: Int) {
        impl.setValue(newValue, changeType, delta)
    }

    private var impl: NumericUpDownImpl

    var orientation: Int = LinearLayout.VERTICAL
        set(value) {
            if (field != value) {
                field = value
                changeDirection(context, null)
            }
        }

    init {
        inflateContent(R.layout.component_number_picker_vertical, true)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        impl = NumericUpDownImpl(context, attrs, this)

        val a = context.theme.obtainStyledAttributes(
                attrs, intArrayOf(android.R.attr.orientation),
                0, 0)

        try {
            if (a.hasValue(0))
                orientation = a.getInteger(0, LinearLayout.VERTICAL)
        } finally {
            a.recycle()
        }
    }

    constructor(context: Context) : super(context) {
        impl = NumericUpDownImpl(context, null, this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        impl.stopFastChange()
    }

    private fun changeDirection(context: Context, attrs: AttributeSet?) {
        removeAllViewsInLayout()
        inflateContent(if (value == LinearLayout.VERTICAL) R.layout.component_number_picker_vertical else R.layout.component_number_picker_horizontal, true)
        requestLayout()
        impl = NumericUpDownImpl(context, attrs, this)
    }


    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val thisState = SavedState(superState)
        thisState.state = impl.makeStateData()
        return thisState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val thisState = state as SavedState
        super.onRestoreInstanceState(thisState.superState)
        impl.applyStateData(thisState.state)
    }

    class SavedState : View.BaseSavedState {

        var state = NumericUpDownImpl.StateData()

        constructor(source: Parcel) : super(source) {
            state.readFromParcel(source)
        }

        @TargetApi(24)
        constructor(source: Parcel, loader: ClassLoader?) : super(source, loader)

        constructor(superState: Parcelable?) : super(superState)


        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            state.writeToParcel(out)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}