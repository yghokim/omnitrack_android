package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.utils.inflateContent

/**
 * Created by Young-Ho on 9/2/2016.
 */
object TrackerPickerDialogHelper {

    fun createDialog(context: Context, inactiveIds: Array<String>? = null, onPicked: ((OTTracker?) -> Unit)): Dialog {

        val view = View.inflate(context, R.layout.simple_layout_with_recycler_view, null)

        val dialog = AlertDialog.Builder(context)
                .setTitle(context.resources.getString(R.string.msg_pick_tracker))
                .setView(view)
                .create()

        val listView = view.findViewById(R.id.ui_list) as RecyclerView

        listView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)


        listView.adapter = TrackerAdapter(inactiveIds) {
            tracker ->
            dialog.dismiss()
            onPicked(tracker)
        }

        return dialog
    }

    class TrackerAdapter(val inactiveIds: Array<String>? = null, val onPicked: ((OTTracker?) -> Unit)) : RecyclerView.Adapter<TrackerAdapter.TrackerViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackerViewHolder {
            val view = parent.inflateContent(R.layout.simple_colored_circle_and_text, false)
            return TrackerViewHolder(view)
        }

        override fun getItemCount(): Int {
            return OTApplication.app.currentUser.trackers.size
        }

        override fun onBindViewHolder(holder: TrackerViewHolder, position: Int) {
            val tracker = OTApplication.app.currentUser.trackers[position]
            holder.circle.setColorFilter(tracker.color)
            holder.textView.text = tracker.name

            holder.active = !(inactiveIds?.contains(tracker.objectId) ?: false)
        }


        inner class TrackerViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

            var active: Boolean = true
                set(value) {
                    field = value
                    if (value == true) {
                        itemView.setOnClickListener(this)
                        itemView.alpha = 1.0f
                    } else {
                        itemView.setOnClickListener(null)
                        itemView.alpha = 0.2f
                    }
                }

            override fun onClick(p0: View?) {
                if (p0 === itemView) {
                    val tracker = OTApplication.app.currentUser.trackers[adapterPosition]
                    onPicked(tracker)
                }
            }

            val circle: AppCompatImageView
            val textView: TextView

            init {
                circle = view.findViewById(R.id.colored_circle) as AppCompatImageView
                textView = view.findViewById(R.id.text) as TextView

                view.setOnClickListener(this)
            }
        }
    }
}