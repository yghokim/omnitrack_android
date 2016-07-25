package kr.ac.snu.hcil.omnitrack.ui.components

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.SpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.text.AttributedCharacterIterator

/**
 * Created by Young-Ho Kim on 2016-07-21.
 */
class AttributeTypeListDialogFragment : DialogFragment() {

    data class AttributeTypeEntry(val typeId: Int, val iconId: Int, val name: String, val description: String?)

    private var currentItemSelectedListener: ((AttributeTypeEntry) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater!!.inflate(R.layout.fragment_attribute_type_list, container)
        val listView = view.findViewById(R.id.ui_list) as RecyclerView

        listView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        listView.adapter = ListAdapter()

        listView.addItemDecoration(SpaceItemDecoration(LinearLayoutManager.VERTICAL, context.resources.getDimensionPixelOffset(R.dimen.list_element_vertical_space)))

        dialog.setTitle(R.string.msg_select_attribute_type)

        return view;
    }

    fun showDialog(fragmentManager: FragmentManager, itemSelectedListener: (AttributeTypeEntry) -> Unit) {
        currentItemSelectedListener = itemSelectedListener
        show(fragmentManager, "dialog")
    }

    private fun onItemClicked(entry: AttributeTypeEntry) {
        if (currentItemSelectedListener != null) {
            currentItemSelectedListener?.invoke(entry)
        }

        dialog.dismiss()
    }

    init {

    }

    inner class ListAdapter() : RecyclerView.Adapter<ListAdapter.ViewHolder>() {

        lateinit var types: Array<AttributeTypeEntry>

        init {
            types = arrayOf(
                    AttributeTypeEntry(OTAttribute.TYPE_NUMBER, R.drawable.field_icon_number, context.getString(R.string.type_number_name), context.getString(R.string.type_number_desc)),
                    AttributeTypeEntry(OTAttribute.TYPE_TIME, R.drawable.field_icon_time, context.getString(R.string.type_timepoint_name), context.getString(R.string.type_timepoint_desc)),
                    AttributeTypeEntry(OTAttribute.TYPE_LONG_TEXT, R.drawable.field_icon_longtext, context.getString(R.string.type_longtext_name), context.getString(R.string.type_longtext_desc))
            )
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.attribute_type_list_element, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(types[position])
        }

        override fun getItemCount(): Int {
            return types.size
        }

        override fun getItemId(position: Int): Long {
            return position.toLong();
        }


        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            lateinit var name: TextView
            lateinit var desc: TextView
            lateinit var icon: ImageView

            init {
                name = view.findViewById(R.id.name) as TextView
                desc = view.findViewById(R.id.description) as TextView
                icon = view.findViewById(R.id.icon) as ImageView

                view.setOnClickListener {
                    onItemClicked(types[adapterPosition])
                }
            }

            fun bind(entry: AttributeTypeEntry) {
                name.text = entry.name
                desc.text = entry.description
                icon.setImageResource(entry.iconId)
            }
        }
    }
}