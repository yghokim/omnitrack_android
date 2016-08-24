package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.ATriggerViewHolder
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.NewTriggerTypeSelectionDialogHelper
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.TimeTriggerViewHolder

/**
 * Created by younghokim on 16. 7. 30..
 */
class TrackerDetailTriggerTabFragment : TrackerDetailActivity.ChildFragment() {


    private lateinit var adapter: Adapter

    private lateinit var listView: RecyclerView

    private lateinit var newTriggerButton: FloatingActionButton

    private val triggerTypeDialog: AlertDialog by lazy {
        NewTriggerTypeSelectionDialogHelper.builder(context) {
            type ->
            println("trigger type selected - $type")
            OTApplication.app.triggerManager.putNewTrigger(
                    OTTrigger.makeInstance(type, "My Trigger", tracker)
            )
            adapter.notifyItemInserted(adapter.itemCount - 1)
            triggerTypeDialog.dismiss()
        }.create()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater!!.inflate(R.layout.fragment_tracker_detail_triggers, container, false)

        listView = rootView.findViewById(R.id.ui_trigger_list) as RecyclerView

        listView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter = Adapter()
        listView.adapter = adapter
        listView.addItemDecoration(HorizontalDividerItemDecoration(resources.getColor(R.color.dividerColor, null), resources.getDimensionPixelSize(R.dimen.trigger_list_element_divider_height)))


        newTriggerButton = rootView.findViewById(R.id.ui_button_new_trigger) as FloatingActionButton

        newTriggerButton.setOnClickListener {
            triggerTypeDialog.show()
        }

        return rootView
    }
/*
    override fun init(tracker: OTTracker, editMode: Boolean) {
        this.tracker = tracker
        this.isEditMode = editMode

        adapter.notifyDataSetChanged()
    }*/

    private fun getTriggers(): Array<OTTrigger> {
        return OTApplication.app.triggerManager.getAttachedTriggers(tracker)
    }

    fun openTriggerDetailActivity(triggerIndex: Int) {

    }

    inner class Adapter : RecyclerView.Adapter<ATriggerViewHolder<out OTTrigger>>(), ATriggerViewHolder.ITriggerControlListener {

        override fun getItemViewType(position: Int): Int {
            return getTriggers()[position].typeId
        }

        override fun onBindViewHolder(holder: ATriggerViewHolder<out OTTrigger>, position: Int) {
            holder.bind(getTriggers()[position])
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ATriggerViewHolder<out OTTrigger> {

            return when (viewType) {
                OTTrigger.TYPE_TIME ->
                    TimeTriggerViewHolder(parent, this, this@TrackerDetailTriggerTabFragment.context)
                else ->
                    TimeTriggerViewHolder(parent, this, this@TrackerDetailTriggerTabFragment.context)

            }
        }

        override fun getItemCount(): Int {
            return getTriggers().size
        }


        override fun onTriggerEdited(position: Int) {

        }

        override fun onTriggerRemove(position: Int) {
            OTApplication.app.triggerManager.removeTrigger(getTriggers()[position])
            this.notifyItemRemoved(position)
        }
    }


}