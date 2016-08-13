package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.HorizontalDividerItemDecoration
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 8/14/2016.
 */
class ChoiceInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<IntArray>(R.layout.input_choice, context, attrs) {

    private val listView: RecyclerView
    private val adapter: Adapter

    private val selectedIndices = ArrayList<Int>()

    init {
        listView = findViewById(R.id.ui_list) as RecyclerView
        adapter = Adapter()

        listView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        listView.adapter = adapter
        listView.addItemDecoration(HorizontalDividerItemDecoration(resources.getColor(R.color.separator_Light, null), (0.8f * (resources.displayMetrics.density + 0.5f)).toInt(), resources.getDimensionPixelSize(R.dimen.choice_indicator_size) + resources.getDimensionPixelSize(R.dimen.choice_indicator_spacing)))

    }

    var entries: Array<String> = arrayOf("Entry 1", "Entry 2", "Entry 3")
        set(value) {
            if (field != value) {
                field = value
                adapter.notifyDataSetChanged()
            }
        }

    override var value: IntArray
        get() = selectedIndices.toIntArray()
        set(value) {
            selectedIndices.clear()
            selectedIndices.addAll(value.toTypedArray())
            adapter.notifyDataSetChanged()
        }


    var multiSelectionMode: Boolean by Delegates.observable(false) {
        prop, old, new ->
        if (old != new) {
            if (new == false && selectedIndices.size > 1) {
                val first = selectedIndices.first()
                selectedIndices.clear()
                selectedIndices.add(first)
            }

            adapter.notifyDataSetChanged()
        }
    }

    override fun focus() {

    }

    override val typeId: Int = VIEW_TYPE_CHOICE


    private inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.choice_entry_list_element, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(entries[position])
        }

        override fun getItemCount(): Int = entries.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), OnClickListener {

            private val indicator: ImageView
            private val textView: TextView

            init {
                indicator = view.findViewById(R.id.ui_checked) as ImageView
                textView = view.findViewById(R.id.ui_text) as TextView

                view.setOnClickListener(this)
            }

            fun bind(entry: String) {
                textView.text = entry
                if (selectedIndices.contains(adapterPosition)) {
                    //checked or selected
                    if (multiSelectionMode) {
                        indicator.setImageResource(R.drawable.toggle_checked)
                    } else {
                        indicator.setImageResource(R.drawable.toggle_selected)
                    }

                    textView.setTextColor(resources.getColor(R.color.textColorMid, null))
                } else {
                    indicator.setImageResource(R.drawable.toggle_empty)
                    textView.setTextColor(resources.getColor(R.color.textColorLight, null))
                }
            }


            override fun onClick(view: View?) {
                if (multiSelectionMode) {
                    if (selectedIndices.contains(adapterPosition)) {
                        selectedIndices.remove(adapterPosition)
                    } else {
                        selectedIndices.add(adapterPosition)
                    }

                    notifyItemChanged(adapterPosition)
                } else {

                    if (!selectedIndices.contains(adapterPosition)) {
                        if (selectedIndices.size > 0) {
                            for (i in selectedIndices.size - 1..0) {
                                val item = selectedIndices[i]
                                selectedIndices.removeAt(i)

                                if (item < entries.size)
                                    notifyItemChanged(item)
                            }
                        }

                        selectedIndices.add(adapterPosition)
                        notifyItemChanged(adapterPosition)
                    }
                }


            }

        }
    }
}