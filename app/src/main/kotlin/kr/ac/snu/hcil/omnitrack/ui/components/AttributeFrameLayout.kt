package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.Button
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.AInputView
import kr.ac.snu.hcil.omnitrack.utils.events.Event

/**
 * Created by Young-Ho Kim on 2016-07-20.
 */
class AttributeFrameLayout : RelativeLayout {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    val editButtonClicked = Event<View>()

    lateinit var previewContainer: ViewGroup
    lateinit var columnNameView: TextView
    lateinit var typeNameView: TextView

    lateinit var editButton: ImageButton

    var preview: AAttributeInputView<out Any>? = null
        get
        set(value) {
            field = value
            if (value != null) {
                previewContainer.addView(value, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            } else {
                previewContainer.removeAllViews()
            }
        }

    override fun onFinishInflate() {
        super.onFinishInflate()
        previewContainer = findViewById(R.id.ui_preview_container) as ViewGroup
        columnNameView = findViewById(R.id.ui_column_name) as TextView
        typeNameView = findViewById(R.id.ui_attribute_type) as TextView
        editButton = findViewById(R.id.ui_button_edit) as ImageButton
        editButton.setOnClickListener {
            editButtonClicked.invoke(this, this)
        }
    }


}