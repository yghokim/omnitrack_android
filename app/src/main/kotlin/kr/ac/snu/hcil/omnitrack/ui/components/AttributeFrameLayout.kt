package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.Button
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.AInputView
import kr.ac.snu.hcil.omnitrack.utils.events.Event

/**
 * Created by Young-Ho Kim on 2016-07-20.
 */
@Deprecated("ViewHolder pattern is used instead.")
class AttributeFrameLayout : RelativeLayout {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    val editButtonClicked = Event<View>()

    lateinit var previewContainer: LockableFrameLayout
    lateinit var columnNameView: TextView
    lateinit var typeNameView: TextView

    lateinit var editButton: ImageButton

    var preview: AAttributeInputView<out Any>? = null
        get
        set(value) {
            if (field !== value) {
                previewContainer.removeAllViews()
                if (value != null) {
                    previewContainer.addView(value, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                }

                field = value
            }
        }

    override fun onFinishInflate() {
        super.onFinishInflate()
        previewContainer = findViewById(R.id.ui_preview_container) as LockableFrameLayout
        previewContainer.locked = true

        columnNameView = findViewById(R.id.ui_column_name) as TextView
        typeNameView = findViewById(R.id.ui_attribute_type) as TextView
        editButton = findViewById(R.id.ui_button_edit) as ImageButton
        editButton.setOnClickListener {
            editButtonClicked.invoke(this, this)
        }
    }

}