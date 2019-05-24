package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kr.ac.snu.hcil.android.common.view.inflateContent
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import org.jetbrains.anko.padding

/**
 * Created by Young-Ho on 9/2/2016.
 */
class TrackerPickerDialogBuilder(val trackers: List<OTTrackerDAO.SimpleTrackerInfo>, val viewHolderFactory: ViewHolderFactory = defaultViewHolderFactory) {

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

    fun createDialog(context: Context, title: Int, inactiveIds: Array<String>? = null, onPicked: (String?) -> Unit): Dialog {
        return createDialog(context, context.resources.getString(title), inactiveIds, onPicked)
    }

    fun createDialog(context: Context, inactiveIds: Array<String>? = null, onPicked: (String?) -> Unit): Dialog {
        return createDialog(context, context.resources.getString(R.string.msg_pick_tracker), inactiveIds, onPicked)
    }

    fun createDialog(context: Context, title: String, inactiveIds: Array<String>? = null, onPicked: (String?) -> Unit): Dialog {

        val view = RecyclerView(context)

        val dialog = AlertDialog.Builder(context)
                .setTitle(title)
                .setView(view)
                .create()

        val listView: RecyclerView = view
        listView.padding = context.resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)
        listView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)


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

        var onPicked: ((String?) -> Unit)? = null

        var trackerId: String? = null

        var active: Boolean = true
            set(value) {
                field = value
                if (value) {
                    itemView.setOnClickListener(this)
                    itemView.alpha = 1.0f
                } else {
                    itemView.setOnClickListener(null)
                    itemView.alpha = 0.2f
                }
            }

        override fun onClick(view: View?) {
            if (view === itemView) {
                onPicked?.invoke(trackerId)
            }
        }

        init {
            view.setOnClickListener(this)
        }

        open fun bind(trackerInfo: OTTrackerDAO.SimpleTrackerInfo) {
            this.trackerId = trackerInfo._id
            circle.setColorFilter(trackerInfo.color)
            textView.text = trackerInfo.name
        }
    }

    inner class TrackerAdapter(val inactiveIds: Array<String>? = null, val onPicked: ((String?) -> Unit)) : RecyclerView.Adapter<TrackerViewHolder>() {

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
            holder.active = inactiveIds?.contains(tracker._id) != true
        }
    }
}