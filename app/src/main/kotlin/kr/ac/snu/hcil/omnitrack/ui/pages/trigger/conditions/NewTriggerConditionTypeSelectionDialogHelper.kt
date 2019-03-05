package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO

/**
 * Created by younghokim on 16. 8. 23..
 */
object NewTriggerConditionTypeSelectionDialogHelper {

    data class TriggerTypeEntry(val typeCode: Byte, @DrawableRes val iconId: Int, @StringRes val nameId: Int, @StringRes val descId: Int, val enabled: Boolean = true)

    private val triggerTypeEntryDict = HashMap<Byte, TriggerTypeEntry>()

    init {
        triggerTypeEntryDict[OTTriggerDAO.CONDITION_TYPE_TIME] = TriggerTypeEntry(OTTriggerDAO.CONDITION_TYPE_TIME, R.drawable.alarm_dark, R.string.trigger_name_time, R.string.trigger_desc_time)
        triggerTypeEntryDict[OTTriggerDAO.CONDITION_TYPE_DATA] = TriggerTypeEntry(OTTriggerDAO.CONDITION_TYPE_DATA, R.drawable.event_dark, R.string.trigger_name_data, R.string.trigger_desc_data)
    }

    fun builder(context: Context, triggerActionTypeName: Int, supportedConditionTypes: Array<Byte>? = null, listener: (Byte) -> Unit): AlertDialog.Builder {

        val view = LayoutInflater.from(context).inflate(R.layout.simple_layout_with_recycler_view, null)
        val listView: RecyclerView = view.findViewById(R.id.ui_recyclerview_with_fallback)

        listView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        listView.adapter = Adapter(supportedConditionTypes, listener)

        return AlertDialog.Builder(context)
                .setTitle(
                        String.format(
                                context.resources.getString(R.string.msg_select_trigger_type_format),
                                context.resources.getString(triggerActionTypeName)
                        ))
                .setView(view)
    }

    private class Adapter(supportedConditionTypes: Array<Byte>? = null, val listener: (Byte) -> Unit) : RecyclerView.Adapter<Adapter.ViewHolder>() {
        val entries = (supportedConditionTypes?.map { triggerTypeEntryDict[it] } ?: triggerTypeEntryDict.values.toList())
                .sortedWith(object : Comparator<TriggerTypeEntry?> {
                    override fun compare(p0: TriggerTypeEntry?, p1: TriggerTypeEntry?): Int {
                        if (p0?.enabled == true && p1?.enabled == false) {
                            return -1
                        } else if (p0?.enabled == p1?.enabled) {
                            return 0
                        } else return 1
                    }
                })


        override fun getItemCount(): Int {
            return entries.size
        }

        private fun getEntryAt(position: Int): TriggerTypeEntry {
            return entries[position]!!
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val element = getEntryAt(position)
            holder.item = element
            holder.iconView.setImageResource(element.iconId)
            holder.nameView.setText(element.nameId)
            holder.descView.setText(element.descId)
            if (element.enabled) {
                holder.iconView.alpha = 1f
                holder.nameView.alpha = 1f
                holder.descView.alpha = 1f
                //holder.disabledMessageView.visibility = View.GONE
            } else {
                holder.iconView.alpha = 0.2f
                holder.nameView.alpha = 0.2f
                holder.descView.alpha = 0.2f
                //holder.disabledMessageView.visibility = View.VISIBLE
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.simple_menu_element_with_icon_title_description, null)
            return ViewHolder(view)
        }


        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
            var item: TriggerTypeEntry? = null

            val iconView: AppCompatImageView = view.findViewById(R.id.icon)
            val nameView: TextView = view.findViewById(R.id.name)
            val descView: TextView = view.findViewById(R.id.description)
            //val disabledMessageView: TextView = view.findViewById(R.id.ui_disabled_message)

            init {
                view.setOnClickListener(this)
            }

            override fun onClick(p0: View?) {
                if (item?.enabled == true)
                    listener.invoke(getEntryAt(adapterPosition).typeCode)
            }
        }

    }
}