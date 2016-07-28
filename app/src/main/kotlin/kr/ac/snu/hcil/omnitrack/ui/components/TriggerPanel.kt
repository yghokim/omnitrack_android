package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.HorizontalDividerItemDecoration
import org.w3c.dom.Text

/**
 * Created by Young-Ho Kim on 2016-07-28.
 */
class TriggerPanel : FrameLayout {

    private var tracker: OTTracker? = null

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private lateinit var expandableView: ExpandableFrameLayout

    private lateinit var summaryView: TextView

    private lateinit var listView: RecyclerView

    private val adapter = Adapter()

    private lateinit var newTriggerButton: View

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.component_trigger_panel, this, true)

        expandableView = findViewById(R.id.root) as ExpandableFrameLayout

        findViewById(R.id.ui_button_expand)?.setOnClickListener {
            expandableView.expand()
        }

        findViewById(R.id.ui_button_collapse)?.setOnClickListener {
            expandableView.collapse()
        }

        summaryView = findViewById(R.id.ui_summary) as TextView

        listView = findViewById(R.id.ui_trigger_list) as RecyclerView

        listView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        listView.adapter = adapter
        listView.addItemDecoration(HorizontalDividerItemDecoration(resources.getColor(R.color.dividerColor, null), resources.getDimensionPixelSize(R.dimen.trigger_list_element_divider_height), context))

        newTriggerButton = findViewById(R.id.ui_button_new_trigger)

        newTriggerButton.setOnClickListener {
            if (tracker != null) {
                OmniTrackApplication.app.triggerManager.putNewTrigger(OTTrigger.makeInstance(OTTrigger.TYPE_PERIODIC, "Trigger ${System.currentTimeMillis()}", tracker!!))
                notifyTriggerSetChanged()
            }
        }

        findViewById(R.id.ui_button_remove_trigger).setOnClickListener {
            if (tracker != null) {
                OmniTrackApplication.app.triggerManager.removeTrigger(getTriggers().last())
                notifyTriggerSetChanged()
            }
        }
    }

    fun attachTracker(tracker: OTTracker) {
        this.tracker = tracker
        notifyTriggerSetChanged()
    }

    private fun getTriggers(): Array<OTTrigger> {
        return OmniTrackApplication.app.triggerManager.getAttachedTriggers(tracker!!)
    }

    fun notifyTriggerSetChanged() {

        summaryView.text = "${OmniTrackApplication.app.triggerManager.getAttachedTriggers(tracker!!).size} Triggers"

        adapter.notifyDataSetChanged()
    }

    fun openTriggerDetailActivity(triggerIndex: Int) {

    }

    inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getTriggers()[position])
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            var view = LayoutInflater.from(context).inflate(R.layout.trigger_list_element, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return getTriggers().size
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            private val typeView: TextView
            private val nameView: TextView
            private val triggerSwitch: Switch

            init {
                typeView = view.findViewById(R.id.type) as TextView
                nameView = view.findViewById(R.id.name) as TextView
                triggerSwitch = view.findViewById(R.id.ui_trigger_switch) as Switch

                view.setOnClickListener {
                    openTriggerDetailActivity(adapterPosition)
                }

                triggerSwitch.setOnClickListener {
                    getTriggers()[adapterPosition].isOn = triggerSwitch.isChecked
                }
            }

            fun bind(trigger: OTTrigger) {
                typeView.text = context.resources.getString(trigger.typeNameResourceId)
                nameView.text = trigger.name
                triggerSwitch.isChecked = trigger.isOn
            }
        }
    }

}