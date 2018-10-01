package kr.ac.snu.hcil.omnitrack.utils

import android.content.Context
import android.support.v7.widget.AppCompatImageView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by younghokim on 16. 8. 24..
 */
class IconNameEntryArrayAdapter(context: Context, objects: Array<out Entry>) : ArrayAdapter<IconNameEntryArrayAdapter.Entry>(context, R.layout.simple_list_element_icon_name, objects) {


    data class Entry(val iconId: Int, val nameId: Int)


    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {

        return getView(position, convertView, parent)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.simple_list_element_icon_name, parent, false)

        if (view.tag !is ViewHolder) {
            view.tag = ViewHolder(view)
        }

        val holder = view.tag as ViewHolder

        holder.iconView.setImageResource(getItem(position).iconId)
        holder.nameView.setText(getItem(position).nameId)

        return view
    }

    class ViewHolder(val view: View) {
        val iconView: AppCompatImageView = view.findViewById(R.id.ui_icon)
        val nameView: TextView = view.findViewById(R.id.ui_name)
    }
}