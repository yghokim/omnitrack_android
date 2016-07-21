package kr.ac.snu.hcil.omnitrack.activities.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.activities.AttributeDetailActivity
import kr.ac.snu.hcil.omnitrack.ui.components.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.properties.ShortTextPropertyView
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-07-21.
 */
class AttributeDetailBasicFragment : Fragment(), AttributeDetailActivity.ChildFragment {
    override var parent: AttributeDetailActivity? = null

    private lateinit var propertyViewContainer: LinearLayout

    private lateinit var columnNameView: ShortTextPropertyView

    private val propertyViewList = ArrayList<Pair<Int?, View>>()

    init {

    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_attribute_detail_basic, container, false)

        propertyViewContainer = rootView.findViewById(R.id.ui_list) as LinearLayout

        columnNameView = rootView.findViewById(R.id.nameProperty) as ShortTextPropertyView

        columnNameView.title = resources.getString(R.string.msg_column_name)

        columnNameView.addNewValidator("Column name cannot be a blank.", ShortTextPropertyView.NOT_EMPTY_VALIDATOR)

        columnNameView.valueChanged += {
            sender, value ->
            if (columnNameView.validate())
                parent?.attribute?.name = value
        }

        refresh()

        return rootView
    }

    override fun refresh() {
        val attr = parent?.attribute
        if (attr != null) {
            columnNameView.value = attr.name ?: ""

            propertyViewContainer.removeAllViewsInLayout()

            val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            propertyViewList.clear()
            for (entry in attr.makePropertyViews(context)) {

                if (entry.first != null) {
                    propertyViewList.add(entry)
                    val propView: APropertyView<Any> = entry.second as APropertyView<Any>

                    propView.value = attr.getPropertyValue(entry.first!!)
                    propView.valueChanged += {
                        sender, value ->
                        if ((sender as APropertyView<out Any>).validate()) {
                            attr.setPropertyValue(entry.first as Int, value)
                        }
                    }
                }

                propertyViewContainer.addView(entry.second, layoutParams)
            }
        }
    }
}