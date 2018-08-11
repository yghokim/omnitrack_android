package kr.ac.snu.hcil.omnitrack.ui.pages.configs

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.AppCompatCheckBox
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import butterknife.bindView
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.di.configured.Backend
import kr.ac.snu.hcil.omnitrack.ui.activities.AppWidgetConfigurationActivity
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.WritablePair
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetUpdateService
import javax.inject.Inject

/**
 * Created by Young-Ho Kim on 2017-04-05.
 */
class ShortcutPanelWidgetConfigActivity : AppWidgetConfigurationActivity(R.layout.activity_config_shortcut_panel_widget) {

    private val modeRadioGroup: RadioGroup by bindView(R.id.ui_radio_group_mode)
    private val titleForm: EditText by bindView(R.id.ui_form_title)

    private val trackerSelectorGroup: ViewGroup by bindView(R.id.ui_group_selected_trackers)
    private val trackerSelectionList: RecyclerView by bindView(R.id.ui_list_selected_trackers)


    private var trackerList: List<WritablePair<OTTrackerDAO.SimpleTrackerInfo, Boolean>>? = null
    private var trackerSelectionAdapter: TrackerSelectionAdapter = TrackerSelectionAdapter()

    @Inject
    lateinit var dbManager: BackendDbManager

    lateinit var realm: Realm


    @field:[Inject Backend]
    lateinit var realmProvider: Factory<Realm>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as OTAndroidApp).currentConfiguredContext.configuredAppComponent.inject(this)
        realm = realmProvider.get()
    }

    override fun onCreateWithWidget(appWidgetId: Int) {
        super.onCreateWithWidget(appWidgetId)

        (findViewById<Button>(R.id.ui_button_bottom_left))
                .apply {
                    this.setOnClickListener {
                        finish()
                    }
                    this.setText(R.string.msg_cancel)
                }

        (findViewById<Button>(R.id.ui_button_bottom_right))
                .apply {
                    this.setOnClickListener {
                        if (initializeWidget(appWidgetId)) {
                            setResult(RESULT_OK)
                            finish()
                        }
                    }
                    this.setText(R.string.msg_ok)
                }

        modeRadioGroup.setOnCheckedChangeListener { radioGroup, checkedId ->
            if (checkedId == R.id.ui_radio_mode_selective) {
                trackerSelectorGroup.visibility = View.VISIBLE
                refreshTrackerSelectionList()
            } else {
                trackerSelectorGroup.visibility = View.GONE
            }
        }

        val pref = OTShortcutPanelWidgetUpdateService.getPreferences(this)
        when (OTShortcutPanelWidgetUpdateService.getMode(appWidgetId, pref)) {
            OTShortcutPanelWidgetUpdateService.MODE_ALL ->
                modeRadioGroup.check(R.id.ui_radio_mode_all)
            OTShortcutPanelWidgetUpdateService.MODE_SELECTIVE ->
                modeRadioGroup.check(R.id.ui_radio_mode_selective)
            OTShortcutPanelWidgetUpdateService.MODE_SHORTCUT ->
                modeRadioGroup.check(R.id.ui_radio_mode_shortcut)
        }
        titleForm.setText(OTShortcutPanelWidgetUpdateService.getTitle(appWidgetId, pref), TextView.BufferType.EDITABLE)

        trackerSelectionList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        trackerSelectionList.adapter = trackerSelectionAdapter
    }

    private fun refreshTrackerSelectionList() {
        creationSubscriptions.add(
                signedInUserObservable.subscribe({
                    userId ->
                    val selectedTrackerIds = OTShortcutPanelWidgetUpdateService.getSelectedTrackerIds(appWidgetId, OTShortcutPanelWidgetUpdateService.getPreferences(this))
                    trackerList = dbManager.makeTrackersOfUserQuery(userId, realm).findAll().map {
                        WritablePair(it.getSimpleInfo(), selectedTrackerIds?.contains(it.objectId) == true)
                    }
                    trackerSelectionAdapter.notifyDataSetChanged()
                }, {

                })
        )
    }

    private fun initializeWidget(appWidgetId: Int): Boolean {

        if (modeRadioGroup.checkedRadioButtonId == R.id.ui_radio_mode_selective) {
            if (trackerList?.filter { it.second == true }?.isNotEmpty() != true) {
                DialogHelper.makeSimpleAlertBuilder(this,
                        getString(R.string.msg_at_least_one_tracker_must_be_selected)).show()
                return false
            }
        }

        val pref = OTShortcutPanelWidgetUpdateService.getPreferences(this)
        val editor = pref.edit()

        OTShortcutPanelWidgetUpdateService.setTitle(appWidgetId, titleForm.text.toString(), editor)
        OTShortcutPanelWidgetUpdateService.setMode(appWidgetId, when (modeRadioGroup.checkedRadioButtonId) {
            R.id.ui_radio_mode_all -> OTShortcutPanelWidgetUpdateService.MODE_ALL
            R.id.ui_radio_mode_selective -> OTShortcutPanelWidgetUpdateService.MODE_SELECTIVE
            R.id.ui_radio_mode_shortcut -> OTShortcutPanelWidgetUpdateService.MODE_SHORTCUT
            else -> throw Exception("ID is wrong.")
        }, editor)

        trackerList?.let {
            OTShortcutPanelWidgetUpdateService.setSelectedTrackerIds(appWidgetId, it.filter { it.second }.map { it.first.objectId!! }.toSet(), editor)
        }
        editor.apply()


        val intent = Intent(this, OTShortcutPanelWidgetUpdateService::class.java).setAction(OTShortcutPanelWidgetUpdateService.ACTION_INITIALIZE)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))

        startService(intent)

        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        trackerList = null
        realm.close()
    }

    inner class TrackerSelectionAdapter : RecyclerView.Adapter<TrackerSelectionAdapter.TrackerViewHolder>() {
        override fun getItemCount(): Int {
            return trackerList?.size ?: 0
        }

        fun getPairAt(position: Int): WritablePair<OTTrackerDAO.SimpleTrackerInfo, Boolean>? {
            return trackerList?.get(position)
        }

        override fun onBindViewHolder(holder: TrackerViewHolder, position: Int) {
            val pair = getPairAt(position)
            if (pair != null) {
                holder.bind(pair)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackerViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_tracker_selectable_list_element, parent, false)
            return TrackerViewHolder(view)
        }

        inner class TrackerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val checkBox: AppCompatCheckBox = view.findViewById(R.id.ui_checkbox)
            val colorBar: View = view.findViewById(R.id.color_bar)

            init {
                checkBox.setOnCheckedChangeListener { compoundButton, checked ->
                    getPairAt(adapterPosition)?.second = checked
                }
            }

            fun bind(pair: WritablePair<OTTrackerDAO.SimpleTrackerInfo, Boolean>) {
                checkBox.text = pair.first.name
                checkBox.isChecked = pair.second
                colorBar.setBackgroundColor(pair.first.color)
            }
        }
    }

}