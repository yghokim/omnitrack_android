package kr.ac.snu.hcil.omnitrack.activities

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ShortTextPropertyView
import kr.ac.snu.hcil.omnitrack.utils.ReadOnlyPair
import java.util.*

class AttributeDetailActivity : MultiButtonActionBarActivity(R.layout.activity_attribute_detail) {

    var attribute: OTAttribute<out Any>? = null


    private lateinit var propertyViewContainer: LinearLayout

    private lateinit var columnNameView: ShortTextPropertyView

    private val propertyViewList = ArrayList<ReadOnlyPair<Int?, View>>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActionBarButtonMode(Mode.Back)

        propertyViewContainer = findViewById(R.id.ui_list) as LinearLayout

        columnNameView = findViewById(R.id.nameProperty) as ShortTextPropertyView
        columnNameView.title = resources.getString(R.string.msg_column_name)

        columnNameView.addNewValidator(String.format(resources.getString(R.string.msg_format_cannot_be_blank), resources.getString(R.string.msg_column_name)), ShortTextPropertyView.NOT_EMPTY_VALIDATOR)

        columnNameView.valueChanged += {
            sender, value ->
            if (columnNameView.validate())
                attribute?.name = value
        }

        refresh()


    }

    override fun onStart() {
        super.onStart()
        if (intent.getStringExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_ATTRIBUTE) != null) {
            attribute = OmniTrackApplication.app.currentUser.findAttributeByObjectId(intent.getStringExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_ATTRIBUTE))
            refresh()
        }
    }

    override fun onLeftButtonClicked() {
        finish()
    }

    override fun onRightButtonClicked() {
    }

    fun refresh() {
        val attr = attribute
        if (attr != null) {
            columnNameView.value = attr.name

            propertyViewContainer.removeAllViewsInLayout()

            val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            layoutParams.bottomMargin = (15 * resources.displayMetrics.density).toInt()

            propertyViewList.clear()
            for (entryWithIndex in attr.makePropertyViews(this).withIndex()) {
                val entry = entryWithIndex.value

                if (entry.first != null) {
                    propertyViewList.add(entry)

                    @Suppress("UNCHECKED_CAST")
                    val propView: APropertyView<Any> = entry.second as APropertyView<Any>

                    propView.value = attr.getPropertyValue(entry.first)
                    propView.valueChanged += {
                        sender, value ->
                        if (sender is APropertyView<*>) {
                            if (sender.validate()) {
                                attr.setPropertyValue(entry.first, value)
                            }
                        }
                    }
                }

                if (entryWithIndex.index == 0) {
                    layoutParams.topMargin = 0
                } else {
                    layoutParams.topMargin = (15 * resources.displayMetrics.density).toInt()
                }

                propertyViewContainer.addView(entry.second, layoutParams)
            }
        }
    }
}
