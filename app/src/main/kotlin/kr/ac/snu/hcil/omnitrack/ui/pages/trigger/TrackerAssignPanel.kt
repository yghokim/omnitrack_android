package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.beloo.widget.chipslayoutmanager.SpacingItemDecoration
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.layout_attached_tracker_list_element_removable.view.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.utils.dipRound
import kr.ac.snu.hcil.omnitrack.utils.getActivity
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import java.util.*

/**
 * Created by Young-Ho on 9/2/2016.
 */
class TrackerAssignPanel : RecyclerView, View.OnClickListener {

    private val addButton: View

    val trackerIds = ArrayList<String>()

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet)

    private val subscriptions = CompositeDisposable()

    init {
        addItemDecoration(SpacingItemDecoration(dipRound(8), dipRound(10)))
        layoutManager = ChipsLayoutManager.newBuilder(context)
                .setChildGravity(Gravity.CENTER_VERTICAL)
                .setOrientation(ChipsLayoutManager.HORIZONTAL)
                .build()

        addButton = inflateContent(R.layout.layout_attached_tracker_list_add, true).findViewById(R.id.ui_button_add)
        addButton.setOnClickListener(this)
    }

    fun init(trackers: Collection<OTTracker>?) {
        trackerIds.clear()
        if (trackers != null)
            trackerIds.addAll(trackers.map { it.objectId })
        refresh()
    }

    private fun refresh() {
/*
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
        }*/

        val activity = getActivity()
        if (activity is OTActivity) {
            /*
            subscriptions.add(
                    activity.signedInUserObservable.subscribe {
                        user ->
                        for (tracker in trackerIds.map { user[it]!! }.withIndex()) {
                            val vh = (getChildAt(tracker.index).tag as RemovableAttachedTrackerViewHolder)
                            vh.textView.text = tracker.value.name
                            vh.colorBar.setBackgroundColor(tracker.value.color)
                        }
                    }
            )*/
        }
    }

    override fun onClick(view: View) {
        val fm = this.getActivity()?.supportFragmentManager
        if (fm != null) {
            /*
            subscriptions.add(

                    OTApplication.instance.currentUserObservable.subscribe {
                        user ->
                        val dialog = TrackerPickerDialogBuilder(user.trackers.unObservedList).createDialog(getActivity()!!, trackerIds.toTypedArray(), {
                            tracker ->
                            if (tracker != null) {
                                trackerIds += tracker.objectId
                                refresh()
                            }
                        })
                        dialog.show()
                    }
            )*/
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        subscriptions.clear()
    }

    private inner class RemovableAttachedTrackerViewHolder(viewParent: ViewGroup?) : AttachedTrackerViewHolder(viewParent, R.layout.layout_attached_tracker_list_element_removable), View.OnClickListener {

        init {
            itemView.ui_button_remove.setOnClickListener(this)
        }

        override fun onClick(clickedView: View) {
            if (clickedView === itemView.ui_button_remove) {
                /*
                val position = this@TrackerAssignPanel.indexOfChild(this.view)
                if (position != -1) {
                    trackerIds.removeAt(position)
                    this@TrackerAssignPanel.removeViewAt(position)
                }*/
            }
        }

    }
}