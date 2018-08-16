package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.actions

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTReminderAction
import kr.ac.snu.hcil.omnitrack.ui.components.common.StringIndicatorPropertyView
import kr.ac.snu.hcil.omnitrack.utils.dipRound

/**
 * Created by younghokim on 2017. 4. 18..
 */
class NotificationSettingsPanelView : StringIndicatorPropertyView, View.OnClickListener {

    companion object {
        const val REQUEST_CODE = 20
    }

    private var selectedNotificationLevel: OTReminderAction.NotificationLevel = OTReminderAction.NotificationLevel.Noti
        set(value) {
            if (field != value) {
                field = value
                indicator = resources.getString(value.nameRes)
            }
        }

    private var overrideAllDevices: Boolean = false

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        title = resources.getString(R.string.msg_notification_level)
        indicator = resources.getString(OTReminderAction.NotificationLevel.Noti.nameRes)
        setOnClickListener(this)
    }

    override fun onClick(v: View) {
        try {
            val dialog = makeLevelSelectionDialog()
            dialog.show()
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

/*
    override fun applyConfigurationToTrigger(trigger: OTTrigger) {
        val action = trigger.triggerAction as? OTReminderAction
        if (action != null) {
            if (overrideAllDevices) {
                action.intrinsicNotificationLevel = selectedNotificationLevel
                action.localNotificationLevel = null
            } else {
                action.localNotificationLevel = selectedNotificationLevel
            }
        }
    }


    override fun writeConfigurationToBundle(out: Bundle) {

    }

    override fun readConfigurationFromBundle(bundle: Bundle) {

    }

    override fun importTriggerConfiguration(trigger: OTTrigger) {
        val action = trigger.triggerAction as? OTReminderAction
        if (action != null) {
            val localLevel = action.localNotificationLevel
            if (localLevel != null) {
                selectedNotificationLevel = localLevel
                overrideAllDevices = false
            } else {
                overrideAllDevices = true
                selectedNotificationLevel = action.intrinsicNotificationLevel
            }
        }
    }

    override fun validateConfigurations(errorMessagesOut: MutableList<String>): Boolean {
        return true
    }*/


    fun makeLevelSelectionDialog(): Dialog {

        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.layout_notification_level_selection_dialog, null, false)

        val list: RecyclerView = view.findViewById(R.id.ui_recyclerview_with_fallback)
        val syncToServerCheckBox: CheckBox = view.findViewById(R.id.ui_checkbox)
        list.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        list.itemAnimator.changeDuration = 0L

        val adapter = LevelAdapter(selectedNotificationLevel.ordinal)
        syncToServerCheckBox.isChecked = overrideAllDevices


        list.adapter = adapter

        return MaterialDialog.Builder(context)
                .customView(view, true)
                .customViewHorizontalPadding(dipRound(context, 8))
                .positiveColorRes(R.color.colorPointed)
                .negativeColorRes(R.color.colorRed_Light)
                .positiveText(R.string.msg_apply)
                .negativeText(R.string.msg_cancel)
                .onPositive {
                    dialog, a ->
                    selectedNotificationLevel = OTReminderAction.NotificationLevel.values()[adapter.selectedLevelIndex]
                    overrideAllDevices = syncToServerCheckBox.isChecked
                }
                .build()
    }


    inner class LevelAdapter(internal var selectedLevelIndex: Int) : RecyclerView.Adapter<LevelAdapter.ViewHolder>() {

        override fun getItemCount(): Int {
            return OTReminderAction.NotificationLevel.values().size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(OTReminderAction.NotificationLevel.values()[position])
            holder.isSelected = selectedLevelIndex == position
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.notification_type_list_element, parent, false)
            return ViewHolder(view)
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val radioView: ImageView = view.findViewById(R.id.ui_radio)
            val thumbnailView: ImageView = view.findViewById(R.id.ui_thumb)
            val titleView: TextView = view.findViewById(R.id.ui_title)
            val descView: TextView = view.findViewById(R.id.ui_description)

            var isSelected: Boolean = false
                set(value) {
                    field = value
                    if (value) {
                        radioView.setImageResource(R.drawable.radiobutton_selected)
                        itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.editTextFormBackground))
                    } else {
                        radioView.setImageResource(R.drawable.radiobutton_empty)
                        itemView.setBackgroundColor(Color.TRANSPARENT)
                    }
                }

            fun bind(level: OTReminderAction.NotificationLevel) {
                thumbnailView.setImageResource(level.thumbnailRes)
                titleView.setText(level.nameRes)
                descView.setText(level.descRes)
            }

            init {
                view.setOnClickListener {
                    if (selectedLevelIndex >= 0) {

                        notifyItemChanged(selectedLevelIndex)
                    }
                    selectedLevelIndex = adapterPosition
                    notifyItemChanged(adapterPosition)
                }
            }

        }

    }

}