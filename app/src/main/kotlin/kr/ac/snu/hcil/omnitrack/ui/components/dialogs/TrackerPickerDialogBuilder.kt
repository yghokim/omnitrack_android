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
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.utils.inflateContent

/**
 * Created by Young-Ho on 9/2/2016.
 */
class TrackerPickerDialogBuilder(val trackers: List<OTTracker>, val viewHolderFactory: ViewHolderFactory = defaultViewHolderFactory) {

    companion object {
        val defaultViewHolderFactory = object : ViewHolderFactory {
            override fun createViewHolder(parent: ViewGroup, viewType: Int): TrackerViewHolder {
                val view = parent.inflateContent(R.layout.simple_colored_circle_and_text, false)
                return TrackerViewHolder(view)
            }
        }
    }

    interface ViewHolderFactory {
        fun createViewHolder(parent: ViewGroup, viewType: Int): TrackerViewHolder
    }

    fun createDialog(context: Context, title: Int, inactiveIds: Array<String>? = null, onPicked: (OTTracker?) -> Unit): Dialog {
        return createDialog(context, context.resources.getString(title), inactiveIds, onPicked)
    }

    fun createDialog(context: Context, inactiveIds: Array<String>? = null, onPicked: (OTTracker?) -> Unit): Dialog {
        return createDialog(context, context.resources.getString(R.string.msg_pick_tracker), inactiveIds, onPicked)
    }

    fun createDialog(context: Context, title: String, inactiveIds: Array<String>? = null, onPicked: (OTTracker?) -> Unit): Dialog {

        val view = View.inflate(context, R.layout.simple_layout_with_recycler_view, null)

        val dialog = AlertDialog.Builder(context)
                .setTitle(title)
                .setView(view)
                .create()

        val listView: RecyclerView = view.findViewById(R.id.ui_list)

        listView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)


        listView.adapter = TrackerAdapter(inactiveIds) {
            tracker ->
            dialog.dismiss()
            onPicked(tracker)
        }

        return dialog
    }

    open class TrackerViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        val circle: AppCompatImageView = view.findViewById(R.id.colored_circle)
        val textView: TextView = view.findViewById(R.id.text)

        var onPicked: ((OTTracker?) -> Unit)? = null

        var tracker: OTTracker? = null

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

        override fun onClick(view: View?) {
            if (view === itemView) {
                onPicked?.invoke(tracker)
            }
        }

        init {
            view.setOnClickListener(this)
        }

        open fun bind(tracker: OTTracker) {
            this.tracker = tracker
            circle.setColorFilter(tracker.color)
            textView.text = tracker.name
        }
    }

    inner class TrackerAdapter(val inactiveIds: Array<String>? = null, val onPicked: ((OTTracker?) -> Unit)) : RecyclerView.Adapter<TrackerViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackerViewHolder {
            val vh = viewHolderFactory.createViewHolder(parent, viewType)
            vh.onPicked = onPicked

            return vh
        }

        override fun getItemCount(): Int {
            return trackers.size
        }

        override fun onBindViewHolder(holder: TrackerViewHolder, position: Int) {
            val tracker = trackers[position]
            holder.bind(tracker)
            holder.active = !(inactiveIds?.contains(tracker.objectId) ?: false)
        }
    }
}