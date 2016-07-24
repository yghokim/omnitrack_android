package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.support.v4.app.ActionBarDrawerToggle
import android.support.v7.widget.ViewStubCompat
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.AInputView
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
abstract class AAttributeInputView<DataType>(layoutId: Int, context: Context, attrs: AttributeSet? = null) : AInputView<DataType>(layoutId, context, attrs) {

    companion object {
        const val VIEW_TYPE_NUMBER = 0
        const val VIEW_TYPE_TIME_POINT = 1
        const val VIEW_TYPE_LONG_TEXT = 2

    }

    abstract val typeId: Int
        get

    private lateinit var columnNameView: TextView
    private lateinit var attributeTypeView: TextView

    private lateinit var headerView: View
    private var headerStub: ViewStubCompat? = null

    private var attribute: OTAttribute<out Any>? = null

    init {

        headerStub = findViewById(R.id.header_stub) as ViewStubCompat

        onSetPreviewMode(true)
    }

    var previewMode: Boolean by Delegates.observable(true) {
        prop, old, new ->
        if (old != new)
            onSetPreviewMode(new)
    }

    protected open fun onSetPreviewMode(mode: Boolean) {
        if (mode) {
            if (headerStub == null) {
                headerView.visibility = View.GONE
            }

            isEnabled = false
            alpha = 0.5f
        } else {
            if (headerStub != null) {
                inflateHeader()
            } else {
                headerView.visibility = View.VISIBLE
            }


            isEnabled = true
            alpha = 1.0f
        }
    }

    open fun bindAttribute(attribute: OTAttribute<out Any>) {
        this.attribute = attribute
        if (headerStub != null) {
            columnNameView.text = attribute.name
            attributeTypeView.text = resources.getString(attribute.typeNameResourceId)
        }
    }

    private fun inflateHeader() {
        if (headerStub != null) {
            headerView = headerStub!!.inflate()
            columnNameView = findViewById(R.id.title) as TextView
            attributeTypeView = findViewById(R.id.ui_attribute_type) as TextView
            if (attribute != null) {
                columnNameView.text = attribute!!.name
                attributeTypeView.text = resources.getString(attribute!!.typeNameResourceId)
            }
        }
    }

}