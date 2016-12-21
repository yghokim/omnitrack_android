package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.animation.LayoutTransition
import android.content.Context
import android.util.AttributeSet
import android.view.View
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.ui.components.dialogs.TrackerPickerDialogBuilder
import kr.ac.snu.hcil.omnitrack.utils.getActivity
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import org.apmem.tools.layouts.FlowLayout
import java.util.*

/**
 * Created by Young-Ho on 9/2/2016.
 */
class TrackerAssignPanel : FlowLayout, View.OnClickListener {

    private val trackerElementCount: Int get() = childCount - 1

    private val addButton: View

    val trackerIds = ArrayList<String>()

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet)

    init {
        addButton = inflateContent(R.layout.layout_attached_tracker_list_add, true).findViewById(R.id.ui_button_add)
        addButton.setOnClickListener(this)
        this.layoutTransition = LayoutTransition()
    }

    fun init(trackers: Collection<OTTracker>) {
        trackerIds.clear()
        trackerIds.addAll(trackers.map { it.objectId })
        refresh()
    }

    private fun refresh() {

        val differ = trackerIds.size - trackerElementCount
        if (differ > 0) {
            for (i in 1..differ) {

                val newView = inflateContent(R.layout.layout_attached_tracker_list_element_removable, false)
                newView.tag = RemovableAttachedTrackerViewHolder(newView)
                addView(newView, 0)
            }
        } else if (differ < 0) {
            for (i in 1..-differ) {
                removeViewAt(trackerElementCount - 1)
            }
        }

        for (tracker in trackerIds.map { OTApplication.app.currentUser[it]!! }.withIndex()) {
            val vh = (getChildAt(tracker.index).tag as RemovableAttachedTrackerViewHolder)
            vh.textView.text = tracker.value.name
            vh.colorBar.setBackgroundColor(tracker.value.color)
        }
    }

    override fun onClick(view: View) {
        val fm = this.getActivity()?.supportFragmentManager
        if (fm != null) {
            val dialog = TrackerPickerDialogBuilder().createDialog(getActivity()!!, trackerIds.toTypedArray(), {
                tracker ->
                if (tracker != null) {
                    trackerIds += tracker.objectId
                    refresh()
                }
            })
            dialog.show()
        }
    }

    private inner class RemovableAttachedTrackerViewHolder(view: View) : ATriggerViewHolder.AttachedTrackerViewHolder(view), View.OnClickListener {

        val removeButton: View

        init {
            removeButton = view.findViewById(R.id.ui_button_remove)
            removeButton.setOnClickListener(this)
        }

        override fun onClick(clickedView: View) {
            if (clickedView === removeButton) {
                val position = this@TrackerAssignPanel.indexOfChild(this.view)
                if (position != -1) {
                    trackerIds.removeAt(position)
                    this@TrackerAssignPanel.removeViewAt(position)
                }
            }
        }

    }
}