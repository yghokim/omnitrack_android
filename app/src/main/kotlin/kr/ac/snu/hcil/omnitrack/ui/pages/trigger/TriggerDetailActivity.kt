package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProviders
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_trigger_detail.*
import kotlinx.android.synthetic.main.layout_tracker_assign_panel.view.*
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerInformationHelper
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTReminderAction
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.TriggerInterfaceOptions
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.onNextIfDifferAndNotNull

/**
 * Created by younghokim on 2017-10-24.
 */
class TriggerDetailActivity : MultiButtonActionBarActivity(R.layout.activity_trigger_detail) {
    companion object {
        const val MODE_NEW = "MODE_NEW_TRIGGER"
        const val MODE_EDIT = "MODE_EDIT_TRIGGER"
        const val INTENT_EXTRA_INTERFACE_OPTIONS = "trigger_interface_options"
        const val INTENT_EXTRA_TRIGGER_DAO = "trigger_dao"

        fun makeNewTriggerIntent(context: Context, baseDao: OTTriggerDAO, options: TriggerInterfaceOptions): Intent {
            val serialized = (context.applicationContext as OTAndroidApp).daoSerializationComponent.manager().serializeTrigger(baseDao)
            println(serialized)
            return Intent(context, TriggerDetailActivity::class.java)
                    .setAction(MODE_NEW)
                    .putExtra(INTENT_EXTRA_TRIGGER_DAO, serialized)
                    .putExtra(INTENT_EXTRA_INTERFACE_OPTIONS, options)
        }

        fun makeEditTriggerIntent(context: Context, triggerId: String, options: TriggerInterfaceOptions): Intent {
            return Intent(context, TriggerDetailActivity::class.java)
                    .setAction(MODE_EDIT)
                    .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER, triggerId)
                    .putExtra(INTENT_EXTRA_INTERFACE_OPTIONS, options)
        }

        fun makeEditTriggerIntent(context: Context, baseDao: OTTriggerDAO, options: TriggerInterfaceOptions): Intent {
            return Intent(context, TriggerDetailActivity::class.java)
                    .setAction(MODE_EDIT)
                    .putExtra(INTENT_EXTRA_TRIGGER_DAO, (context.applicationContext as OTAndroidApp).daoSerializationComponent.manager().serializeTrigger(baseDao))
                    .putExtra(INTENT_EXTRA_INTERFACE_OPTIONS, options)
        }
    }

    private lateinit var interfaceOptions: TriggerInterfaceOptions
    private lateinit var viewModel: TriggerDetailViewModel

    private var trackerAssignPanelContainer: View? = null
    private var trackerAssignPanel: TrackerAssignPanel? = null

    private var conditionConfigurationView: IConditionConfigurationView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setActionBarButtonMode(Mode.ApplyCancel)

        ui_script_form.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus)
                viewModel.script.onNextIfDifferAndNotNull(Nullable(ui_script_form.text.toString().let { if (it.isBlank()) null else it }))
        }

        creationSubscriptions.add(
                signedInUserObservable.subscribe { userId ->
                    viewModel = ViewModelProviders.of(this).get(TriggerDetailViewModel::class.java)
                    interfaceOptions = intent.getSerializableExtra(INTENT_EXTRA_INTERFACE_OPTIONS) as TriggerInterfaceOptions
                    val mode = intent.action
                    when (mode) {
                        MODE_NEW -> {
                            val baseDao = (application as OTAndroidApp).daoSerializationComponent.manager().parseTrigger(intent.getStringExtra(INTENT_EXTRA_TRIGGER_DAO))
                            viewModel.initNew(baseDao, savedInstanceState)
                        }
                        MODE_EDIT -> {
                            if (intent.hasExtra(INTENT_EXTRA_TRIGGER_DAO)) {
                                val baseDao = (application as OTAndroidApp).daoSerializationComponent.manager().parseTrigger(intent.getStringExtra(INTENT_EXTRA_TRIGGER_DAO))
                                viewModel.initEdit(baseDao, savedInstanceState)
                            } else if (intent.hasExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER)) {
                                viewModel.initEdit(intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER), userId, savedInstanceState)
                            }
                        }
                    }

                    connectViewsToViewModel(viewModel)

                    if (interfaceOptions.showAttachedTrackers) {
                        if (trackerAssignPanelContainer == null) {
                            trackerAssignPanelContainer = ui_tracker_assign_panel_stub.inflate()
                            trackerAssignPanel = trackerAssignPanelContainer?.ui_tracker_assign_list
                            trackerAssignPanel?.let {
                                if (viewModel.attachedTrackers.hasValue()) {
                                    it.init(viewModel.attachedTrackers.value!!)
                                }

                                creationSubscriptions.add(
                                        it.trackerListChanged.subscribe { newList ->
                                            viewModel.attachedTrackers.onNext(newList)
                                        }
                                )
                            }
                        }
                    } else trackerAssignPanelContainer?.visibility = View.GONE

                    if (interfaceOptions.defaultActionType == OTTriggerDAO.ACTION_TYPE_REMIND) {
                        //if remind, show reminder durationSeconds
                        ui_reminder_config_view.visibility = View.VISIBLE

                        val currentAction = viewModel.actionInstance.value as? OTReminderAction
                        if (currentAction != null) {
                            ui_reminder_config_view.expiry = currentAction.expirySeconds
                            ui_reminder_config_view.message = currentAction.message
                        }

                        creationSubscriptions.add(ui_reminder_config_view.expirySubject.subscribe { expiry ->
                            val currentAction = viewModel.actionInstance.value as? OTReminderAction
                            if (currentAction != null && currentAction.expirySeconds != expiry) {
                                viewModel.actionInstance.onNext((currentAction.clone() as OTReminderAction).apply { this.expirySeconds = expiry })
                            }
                        })

                        creationSubscriptions.add(ui_reminder_config_view.messageSubject.subscribe { (message) ->
                            val currentAction = viewModel.actionInstance.value as? OTReminderAction
                            if (currentAction != null && currentAction.message != message) {
                                viewModel.actionInstance.onNext((currentAction.clone() as OTReminderAction).apply { this.message = message })
                            }
                        })

                    } else {
                        ui_reminder_config_view.visibility = View.GONE
                    }
                }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.onSaveInstanceState(outState)
    }

    private fun connectViewsToViewModel(viewModel: TriggerDetailViewModel) {
        creationSubscriptions.addAll(
                viewModel.actionType.subscribe { actionType ->
                    val actionNameResId = OTTriggerInformationHelper.getActionNameResId(actionType)
                    if (viewModel.viewModelMode.hasValue() && actionNameResId != null) {
                        if (viewModel.viewModelMode.value == TriggerDetailViewModel.MODE_NEW) {
                            title = String.format(getString(R.string.msg_format_new_something), getString(actionNameResId))
                        } else {
                            title = String.format(getString(R.string.msg_format_edit_something), getString(actionNameResId))
                        }
                    }
                },

                viewModel.conditionType.subscribe { conditionType ->
                    val viewProvider = OTTriggerViewFactory.getConditionViewProvider(conditionType)
                    if (viewProvider != null) {
                        val configView = viewProvider.getTriggerConfigurationPanel(conditionConfigurationView as? View, this)
                        this.conditionConfigurationView = configView
                        ui_condition_control_panel_container.removeAllViewsInLayout()
                        ui_condition_control_panel_container.addView(configView as View, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                        creationSubscriptions.add(
                                viewModel.conditionInstance.subscribe { condition ->
                                    configView.applyCondition(condition)
                                }
                        )
                        creationSubscriptions.add(
                                configView.onConditionChanged.subscribe { newCondition ->
                                    viewModel.conditionInstance.onNext(newCondition)
                                }
                        )
                    }
                },

                viewModel.useScript.subscribe { uses ->
                    ui_use_script_property_view.value = uses
                    ui_script_form.isEnabled = uses
                },

                viewModel.script.subscribe { (scriptText) ->
                    val text = scriptText ?: ""
                    if (ui_script_form.text.toString().trim() != text) {
                        ui_script_form.setText(text, TextView.BufferType.EDITABLE)
                    }
                },

                ui_use_script_property_view.valueChanged.observable.subscribe { (sender, uses) ->
                    viewModel.useScript.onNextIfDifferAndNotNull(uses)
                },

                viewModel.attachedTrackers.subscribe { trackerInfos ->
                    trackerAssignPanel?.init(trackerInfos)
                }
        )
    }

    override fun onToolbarLeftButtonClicked() {
        ui_script_form.clearFocus()
        if (viewModel.isDirty) {
            val isEditMode = viewModel.viewModelMode.value == TriggerDetailViewModel.MODE_EDIT
            val actionNameId = OTTriggerInformationHelper.getActionNameResId(viewModel.actionType.value
                    ?: 0) ?: R.string.msg_text_trigger
            val msg = if (isEditMode) {
                String.format(getString(R.string.msg_format_confirm_apply_change), getString(actionNameId).toLowerCase())
            } else {
                String.format(getString(R.string.msg_format_confirm_save_creation), getString(actionNameId).toLowerCase())
            }

            DialogHelper.makeYesNoDialogBuilder(this, "OmniTrack",
                    msg, yesLabel = R.string.msg_save, noLabel = R.string.msg_do_not_save, onYes =
            {
                creationSubscriptions.add(
                        viewModel.validateConfiguration(this).observeOn(AndroidSchedulers.mainThread()).subscribe { (valid, errorMessages) ->
                            if (valid) {
                                viewModel.saveFrontToDao()
                                setResult(Activity.RESULT_OK, makeResultData())
                                finish()
                            } else {
                                DialogHelper.makeSimpleAlertBuilder(this, errorMessages?.joinToString("\n")
                                        ?: "Configuration is not valid.").show()
                            }
                        }
                )
            }, onNo = { setResult(Activity.RESULT_CANCELED); finish() })
                    .cancelable(true)
                    .neutralText(R.string.msg_cancel).show()
        } else {
            finish()
        }
    }

    override fun onBackPressed() {
        onToolbarLeftButtonClicked()
    }

    override fun onToolbarRightButtonClicked() {
        ui_script_form.clearFocus()
        creationSubscriptions.add(
                viewModel.validateConfiguration(this).subscribe { (valid, errorMessages) ->
                    if (valid) {
                        viewModel.saveFrontToDao()
                        setResult(Activity.RESULT_OK, makeResultData())
                        finish()
                    } else {
                        DialogHelper.makeSimpleAlertBuilder(this, errorMessages?.joinToString("\n")
                                ?: "Configuration is not valid.").show()
                    }
                }
        )
    }

    private fun makeResultData(): Intent {
        return Intent(intent).apply {
            if (viewModel.isOffline) {
                //offline mode
                putExtra(INTENT_EXTRA_TRIGGER_DAO, viewModel.getSerializedDao())
            }
        }
    }

}