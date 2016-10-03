package kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard.pages

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ComboBoxPropertyView
import kr.ac.snu.hcil.omnitrack.utils.events.IEventListener
import kr.ac.snu.hcil.omnitrack.utils.inflateContent

/**
 * Created by Young-Ho Kim on 2016-09-30.
 */
class SourceConfigurationPanel : FrameLayout, IEventListener<Int> {

    private val queryPresetSelectionView: ComboBoxPropertyView

    private val queryPresetAdapter = PresetAdapter(OTTimeRangeQuery.Preset.values().toList())

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        inflateContent(R.layout.connection_source_configuration_panel, true)

        queryPresetSelectionView = findViewById(R.id.ui_preset_selection) as ComboBoxPropertyView

        queryPresetSelectionView.adapter = queryPresetAdapter

        queryPresetSelectionView.valueChanged += this
    }

    fun applyConfiguration(connection: OTConnection) {
        connection.rangedQuery = queryPresetAdapter.getItem(queryPresetSelectionView.value).makeQueryInstance()
    }

    override fun onEvent(sender: Any, args: Int) {
        if (sender === queryPresetSelectionView) {

        }
    }

    inner class PresetAdapter(presets: List<OTTimeRangeQuery.Preset>) : ArrayAdapter<OTTimeRangeQuery.Preset>(context, R.layout.simple_list_element_name_desc_dropdown, presets) {

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {

            val view = getView(position, convertView, parent)
            view.setBackgroundResource(R.drawable.bottom_separator_thin)

            return view
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val view = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.simple_list_element_name_desc_dropdown, parent, false)

            if (view.tag !is ViewHolder) {
                view.tag = ViewHolder(view)
            }

            val holder = view.tag as ViewHolder

            holder.descriptionView.setText(getItem(position).descResId)
            holder.nameView.setText(getItem(position).nameResId)

            view.background = null

            return view
        }

        inner class ViewHolder(val view: View) {

            val nameView: TextView
            val descriptionView: TextView

            init {
                descriptionView = view.findViewById(R.id.description) as TextView
                nameView = view.findViewById(R.id.name) as TextView
            }

        }
    }

}