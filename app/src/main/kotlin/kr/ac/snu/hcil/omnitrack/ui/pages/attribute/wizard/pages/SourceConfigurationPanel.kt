package kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard.pages

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.ui.components.common.viewholders.SimpleNameDescriptionViewHolder
import kr.ac.snu.hcil.omnitrack.ui.components.common.viewholders.SimpleTextViewHolder
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

        queryPresetSelectionView = findViewById(R.id.ui_preset_selection)

        queryPresetSelectionView.adapter = queryPresetAdapter

        queryPresetSelectionView.valueChanged += this
    }

    fun initialize(pendingConnection: OTConnection) {

        val filtered = OTTimeRangeQuery.Preset.values().filter { it.granularity.ordinal >= pendingConnection.source?.factory?.minimumGranularity?.ordinal ?: 0 }
        println(filtered)

        queryPresetAdapter.clear()
        queryPresetAdapter.addAll(filtered)
        queryPresetAdapter.notifyDataSetChanged()
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

            val view = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.simple_list_element_name_desc_dropdown, parent, false)

            if (view.tag !is SimpleNameDescriptionViewHolder) {
                view.tag = SimpleNameDescriptionViewHolder(view)
            }

            val holder = view.tag as SimpleNameDescriptionViewHolder

            holder.descriptionView.setText(getItem(position).descResId)
            holder.nameView.setText(getItem(position).nameResId)

            view.setBackgroundResource(R.drawable.bottom_separator_thin)

            return view
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val view = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.simple_list_element_text_dropdown, parent, false)

            if (view.tag !is SimpleTextViewHolder) {
                view.tag = SimpleTextViewHolder(view)
            }

            val holder = view.tag as SimpleTextViewHolder

            holder.textView.setText(getItem(position).nameResId)

            view.background = null

            return view
        }
    }

}