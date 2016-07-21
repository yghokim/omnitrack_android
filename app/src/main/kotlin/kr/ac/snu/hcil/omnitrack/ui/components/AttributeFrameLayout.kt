package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewParent
import android.widget.Button
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTAttribute
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

    lateinit var previewContainer: ViewParent
    lateinit var columnNameView: TextView
    lateinit var typeNameView: TextView

    lateinit var editButton: ImageButton

    override fun onFinishInflate() {
        super.onFinishInflate()
        columnNameView = findViewById(R.id.ui_column_name) as TextView
        typeNameView = findViewById(R.id.ui_attribute_type) as TextView
        editButton = findViewById(R.id.ui_button_edit) as ImageButton
        editButton.setOnClickListener {
            editButtonClicked.invoke(this, this)
        }
    }
}