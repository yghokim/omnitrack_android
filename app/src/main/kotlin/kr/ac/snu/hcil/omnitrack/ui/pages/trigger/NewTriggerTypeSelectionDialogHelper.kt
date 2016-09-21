package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger

/**
 * Created by younghokim on 16. 8. 23..
 */
object NewTriggerTypeSelectionDialogHelper {

    data class TriggerTypeEntry(val typeCode: Int, val iconId: Int, val nameId: Int, val descId: Int)

    val triggerTypes = arrayOf(
            TriggerTypeEntry(OTTrigger.TYPE_TIME, R.drawable.alarm_dark, R.string.trigger_name_time, R.string.trigger_desc_time),
            TriggerTypeEntry(OTTrigger.TYPE_SERVICE_EVENT, R.drawable.event_dark, R.string.trigger_name_event, R.string.trigger_desc_event)
    )

    fun builder(context: Context, triggerActionTypeName: Int, listener: (Int) -> Unit): AlertDialog.Builder {

        val view = LayoutInflater.from(context).inflate(R.layout.simple_layout_with_recycler_view, null)
        val listView = view.findViewById(R.id.ui_list) as RecyclerView

        listView.layoutManager = GridLayoutManager(context, 2)
        listView.adapter = Adapter(listener)

        return AlertDialog.Builder(context)
                .setTitle(
                        String.format(
                                context.resources.getString(R.string.msg_select_trigger_type_format),
                                context.resources.getString(triggerActionTypeName)
                        ))
                .setView(view)
    }

    private class Adapter(val listener: (Int) -> Unit) : RecyclerView.Adapter<Adapter.ViewHolder>() {

        override fun getItemCount(): Int {
            return triggerTypes.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.iconView.setImageResource(triggerTypes[position].iconId)
            holder.nameView.setText(triggerTypes[position].nameId)
            holder.descView.setText(triggerTypes[position].descId)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_trigger_type_selection_element, null)
            return ViewHolder(view)
        }


        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

            val iconView: AppCompatImageView
            val nameView: TextView
            val descView: TextView

            init {
                iconView = view.findViewById(R.id.ui_icon) as AppCompatImageView
                nameView = view.findViewById(R.id.ui_name) as TextView
                descView = view.findViewById(R.id.ui_description) as TextView
                view.setOnClickListener(this)
            }

            override fun onClick(p0: View?) {
                listener.invoke(triggerTypes[adapterPosition].typeCode)
            }
        }

    }
}