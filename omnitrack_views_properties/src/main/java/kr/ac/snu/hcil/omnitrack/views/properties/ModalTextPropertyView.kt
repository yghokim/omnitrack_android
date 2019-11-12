package kr.ac.snu.hcil.omnitrack.views.properties

import android.annotation.TargetApi
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.android.synthetic.main.component_property_modaltext.view.*

/**
 * Created by younghokim on 2017. 11. 10..
 */
class ModalTextPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<String>(R.layout.component_property_modaltext, context, attrs), View.OnClickListener {

    private var builderCache: MaterialDialog.Builder? = null

    private val dialogBuilder: MaterialDialog.Builder
        get() {
            if (builderCache == null) {
                builderCache = MaterialDialog.Builder(context)
                        .title(dialogTitle)
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .inputRangeRes(inputLengthMin, inputLengthMax, R.color.colorRed)
                        .cancelable(true)
                        .negativeText(R.string.msg_cancel)
                        .input(hint, value, inputLengthMin == 0) { dialog, input ->
                            if (value != input) {
                                val string = input.toString()
                                value = string
                                onValueChanged(value)
                            }
                        }
            }
            return builderCache!!
        }

    override var value: String = ""
        set(value) {
            if (field != value) {
                field = value
                ui_text.text = value
                builderCache = null
            }
        }

    var inputLengthMin: Int = 0
        set(value) {
            field = value
            builderCache = null
        }

    var inputLengthMax: Int = -1
        set(value) {
            field = value
            builderCache = null
        }


    var dialogTitle: String = ""
        set(value) {
            field = value
            builderCache = null
        }

    var hint: String? = null
        set(value) {
            field = value
            builderCache = null
            ui_text.hint = value
        }


    init {
        ui_text.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        if (isEnabled) {
            if (view === ui_text) {
                dialogBuilder.show()
            }
        }
    }


    override fun getSerializedValue(): String? {
        return value
    }

    override fun setSerializedValue(serialized: String): Boolean {
        try {
            value = serialized
            return true
        } catch (e: Exception) {
            return false
        }
    }

    override fun focus() {

    }


    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val thisState = SavedState(superState)
        thisState.dialogTitle = dialogTitle
        thisState.emptyFallback = hint
        thisState.inputLengthMin = inputLengthMin
        thisState.inputLengthMax = inputLengthMax
        return thisState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val thisState = state as SavedState
        super.onRestoreInstanceState(thisState.superState)
        dialogTitle = thisState.dialogTitle
        hint = thisState.emptyFallback
        inputLengthMin = thisState.inputLengthMin
        inputLengthMax = thisState.inputLengthMax
    }

    class SavedState : BaseSavedState {

        var dialogTitle: String = ""
        var emptyFallback: String? = null
        var inputLengthMin: Int = 0
        var inputLengthMax: Int = 0

        constructor(source: Parcel) : super(source) {
            dialogTitle = source.readString() ?: ""
            emptyFallback = source.readString()
            inputLengthMin = source.readInt()
            inputLengthMax = source.readInt()
        }

        @TargetApi(24)
        constructor(source: Parcel, loader: ClassLoader?) : super(source, loader)

        constructor(superState: Parcelable?) : super(superState)


        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeString(dialogTitle)
            out.writeString(emptyFallback)
            out.writeInt(inputLengthMin)
            out.writeInt(inputLengthMax)
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