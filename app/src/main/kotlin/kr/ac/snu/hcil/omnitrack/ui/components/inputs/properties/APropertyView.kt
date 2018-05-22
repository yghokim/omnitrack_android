package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-07-13.
 */
abstract class APropertyView<T>(layoutId: Int, context: Context, attrs: AttributeSet?) : AInputView<T>(layoutId, context, attrs) {

    protected var titleView: TextView = findViewById(R.id.title)

    var useIntrinsicPadding: Boolean = false

    var showEditedOnTitle: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                refreshTitle()
            }
        }

    private var titleBody: CharSequence = ""
    var title: CharSequence
        get() = titleBody
        set(value) {
            if (titleBody != value) {
                titleBody = value
                refreshTitle()
            }
        }

    abstract fun getSerializedValue(): String?
    abstract fun setSerializedValue(serialized: String): Boolean

    constructor(layoutId: Int, context: Context) : this(layoutId, context, null)

    init {

        val a = context.theme.obtainStyledAttributes(
                attrs, intArrayOf(android.R.attr.text),
                0, 0)

        try {
            if (a.hasValue(0))
                title = a.getString(0)
        } finally {
            a.recycle()
        }

    }

    private fun refreshTitle() {
        if (titleBody.isBlank()) {
            titleView.visibility = View.GONE
        } else {
            titleView.visibility = View.VISIBLE
            titleView.text = if (showEditedOnTitle) "$titleBody*" else titleBody
        }
    }

    open fun compareAndShowEditedAny(comparedTo: Any?) {
        try {
            compareAndShowEdited(comparedTo as T)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    open fun compareAndShowEdited(comparedTo: T) {
        showEditedOnTitle = comparedTo != value
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val thisState = state as SavedState
        super.onRestoreInstanceState(thisState.superState)
        title = thisState.title ?: ""
        thisState.serializedValue?.let {
            setSerializedValue(it)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val thisState = SavedState(superState)
        thisState.title = title.toString()
        thisState.serializedValue = getSerializedValue()
        return thisState
    }

    open class SavedState : BaseSavedState {

        var title: String? = null
        var serializedValue: String? = null

        constructor(source: Parcel) : super(source) {
            title = source.readString()
            serializedValue = source.readString()
        }

        constructor(superState: Parcelable) : super(superState)

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeString(title)
            out.writeString(serializedValue)
        }
    }
}