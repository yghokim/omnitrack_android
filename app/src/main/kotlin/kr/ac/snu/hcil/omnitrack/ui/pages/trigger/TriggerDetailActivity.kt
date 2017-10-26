package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_trigger_detail.*
import kotlinx.android.synthetic.main.layout_tracker_assign_panel.view.*
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.OTTriggerInformationHelper
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.TriggerInterfaceOptions
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper

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
            val serialized = OTTriggerDAO.parser.toJson(baseDao, OTTriggerDAO::class.java)
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
                    .putExtra(INTENT_EXTRA_TRIGGER_DAO, OTTriggerDAO.parser.toJson(baseDao, OTTriggerDAO::class.java))
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

        creationSubscriptions.add(
                signedInUserObservable.subscribe { userId ->
                    viewModel = ViewModelProviders.of(this).get(TriggerDetailViewModel::class.java)
                    interfaceOptions = intent.getSerializableExtra(INTENT_EXTRA_INTERFACE_OPTIONS) as TriggerInterfaceOptions
                    if (savedInstanceState == null) {
                        val mode = intent.action
                        when (mode) {
                            MODE_NEW -> {
                                val baseDao = OTTriggerDAO.parser.fromJson(intent.getStringExtra(INTENT_EXTRA_TRIGGER_DAO), OTTriggerDAO::class.java)
                                viewModel.initNew(baseDao)
                            }
                            MODE_EDIT -> {
                                if (intent.hasExtra(INTENT_EXTRA_TRIGGER_DAO)) {
                                    val baseDao = OTTriggerDAO.parser.fromJson(intent.getStringExtra(INTENT_EXTRA_TRIGGER_DAO), OTTriggerDAO::class.java)
                                    viewModel.initEdit(baseDao)
                                } else if (intent.hasExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER)) {
                                    viewModel.initEdit(intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER), userId)
                                }
                            }
                        }
                    }


                    connectViewsToViewModel(viewModel)

                    if (interfaceOptions.showAttachedTrackers) {
                        if (trackerAssignPanelContainer == null) {
                            trackerAssignPanelContainer = ui_tracker_assign_panel_stub.inflate()
                            trackerAssignPanel = trackerAssignPanelContainer?.ui_tracker_assign_list
                        }
                    } else trackerAssignPanelContainer?.visibility = View.GONE
                }
        )
    }

    private fun connectViewsToViewModel(viewModel: TriggerDetailViewModel) {
        creationSubscriptions.addAll(
                viewModel.actionType.subscribe { actionType ->
                    val actionNameResId = OTTriggerInformationHelper.getActionNameResId(actionType)
                    if (viewModel.viewModelMode.hasValue() && actionNameResId != null) {
                        if (viewModel.viewModelMode.value == TriggerDetailViewModel.MODE_NEW) {
                            title = String.format(OTApp.getString(R.string.msg_format_new_something), OTApp.getString(actionNameResId))
                        } else {
                            title = String.format(OTApp.getString(R.string.msg_format_edit_something), OTApp.getString(actionNameResId))
                        }
                    }
                },

                viewModel.conditionType.subscribe { conditionType ->
                    val viewProvider = OTTriggerViewFactory.getConditionViewProvider(conditionType)
                    if (viewProvider != null) {
                        val configView = viewProvider.getTriggerConfigurationPanel(conditionConfigurationView as? View, this)
                        this.conditionConfigurationView = configView
                        ui_condition_control_panel_container.removeAllViewsInLayout()
                        ui_condition_control_panel_container.addView(configView as View)
                        creationSubscriptions.add(
                                viewModel.conditionInstance.subscribe { condition ->
                                    configView.applyCondition(condition)
                                }
                        )
                        creationSubscriptions.add(
                                configView.onConditionChanged.subscribe { newCondition ->
                                    println("condition changed - ${newCondition.getSerializedString()}")
                                    viewModel.conditionInstance.onNext(newCondition)
                                }
                        )
                    }
                }
        )
    }

    override fun onToolbarLeftButtonClicked() {
        if (viewModel.isDirty) {
            DialogHelper.makeYesNoDialogBuilder(this, "OmniTrack",
                    resources.getString(R.string.msg_confirm_trigger_apply_change), R.string.msg_apply, onYes =
            {
                val errorMessages = viewModel.validateConfiguration()
                if (errorMessages == null) {
                    viewModel.saveFrontToDao()
                    setResult(Activity.RESULT_OK, makeResultData())
                    finish()
                } else {
                    DialogHelper.makeSimpleAlertBuilder(this, errorMessages.joinToString("\n")).show()
                }
            }, onNo = { setResult(Activity.RESULT_CANCELED); finish() }).show()
        } else {
            finish()
        }
    }

    override fun onToolbarRightButtonClicked() {
        val errorMessages = viewModel.validateConfiguration()
        if (errorMessages == null) {
            viewModel.saveFrontToDao()
            setResult(Activity.RESULT_OK, makeResultData())
            finish()
        } else {
            DialogHelper.makeSimpleAlertBuilder(this, errorMessages.joinToString("\n")).show()
        }
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