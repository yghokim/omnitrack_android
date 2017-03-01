package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import butterknife.bindView
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import rx.internal.util.SubscriptionList
import java.util.*

/**
 * Created by younghokim on 2017. 2. 21..
 */
class TriggerDetailActivity : MultiButtonActionBarActivity(R.layout.activity_trigger_detail) {

    companion object {
        const val INTENT_EXTRA_TRIGGER_TYPE = "trigger_type"
        const val INTENT_EXTRA_TRIGGER_ACTION = "trigger_action"
        const val INTENT_EXTRA_HIDE_ATTACHED_TRACKERS = "hide_attached_trackers"
        const val INTENT_EXTRA_TRIGGER_DATA = "trigger_data"

        fun makeNewTriggerIntent(context: Context, triggerType: Int, triggerAction: Int, hideAttachedTrackers: Boolean = false): Intent {
            val intent = Intent(context, TriggerDetailActivity::class.java)
                    .putExtra(INTENT_EXTRA_TRIGGER_TYPE, triggerType)
                    .putExtra(INTENT_EXTRA_TRIGGER_ACTION, triggerAction)
                    .putExtra(INTENT_EXTRA_HIDE_ATTACHED_TRACKERS, hideAttachedTrackers)
            return intent
        }

        fun makeEditTriggerIntent(context: Context, trigger: OTTrigger, hideAttachedTrackers: Boolean = false): Intent {
            val intent = Intent(context, TriggerDetailActivity::class.java)
                    .putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRIGGER, trigger.objectId)
                    .putExtra(INTENT_EXTRA_HIDE_ATTACHED_TRACKERS, hideAttachedTrackers)
            return intent
        }
    }

    private var triggerId: String? = null
    private var attachedTrigger: OTTrigger? = null

    private var triggerType: Int = -1
    private var triggerAction: Int = -1

    private var user: OTUser? = null

    private val isEditMode: Boolean get() = triggerId != null

    private var configurationCoordinatorView: ITriggerConfigurationCoordinator? = null

    private val controlPanelContainer: ViewGroup by bindView(R.id.ui_control_panel)
    private val trackerAssignPanelStub: ViewStub by bindView(R.id.ui_tracker_assign_panel_stub)
    private var trackerAssignPanelContainer: View? = null
    private var trackerAssignPanel: TrackerAssignPanel? = null

    private var hideAttachedTrackers: Boolean = false

    private val errorMessages = ArrayList<String>()

    private var startSubscriptions = SubscriptionList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActionBarButtonMode(Mode.SaveCancel)

        if (intent.hasExtra(INTENT_EXTRA_HIDE_ATTACHED_TRACKERS)) {
            hideAttachedTrackers = intent.getBooleanExtra(INTENT_EXTRA_HIDE_ATTACHED_TRACKERS, false)
        }

        creationSubscriptions.add(
                signedInUserObservable.subscribe {
                    user ->
                    this.user = user
                    if (intent.hasExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRIGGER)) {
                        val triggerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRIGGER)
                        attachedTrigger = user.triggerManager.getTriggerWithId(triggerId)
                        this.triggerId = triggerId
                        triggerType = attachedTrigger?.typeId ?: -1
                        triggerAction = attachedTrigger?.action ?: -1
                        //attachedTrigger?.dumpDataToPojo(null)?.toMutable(currentTriggerPojo)

                        title = resources.getString(R.string.title_activity_trigger_edit)
                        setActionBarButtonMode(Mode.ApplyCancel)
                    } else {
                        triggerType = intent.getIntExtra(INTENT_EXTRA_TRIGGER_TYPE, -1)
                        triggerAction = intent.getIntExtra(INTENT_EXTRA_TRIGGER_ACTION, -1)

                        title = resources.getString(R.string.title_activity_trigger_new)
                        setActionBarButtonMode(Mode.SaveCancel)

                    }


                    onUserLoaded(user)
                }
        )
    }

    override fun onToolbarLeftButtonClicked() {
        if (isEditMode) {
            DialogHelper.makeYesNoDialogBuilder(this, "OmniTrack",
                    resources.getString(R.string.msg_confirm_trigger_apply_change),
                    {
                        if (validateConfigurations()) {
                            attachedTrigger?.let {
                                applyViewToTrigger(it)
                            }
                            setResult(Activity.RESULT_OK)
                            finish()
                        } else {
                            DialogHelper.makeSimpleAlertBuilder(this, errorMessages.joinToString("\n")).show()
                        }
                    }, { setResult(Activity.RESULT_CANCELED); finish() }).show()
        } else {
            DialogHelper.makeYesNoDialogBuilder(this, "OmniTrack",
                    resources.getString(R.string.msg_confirm_trigger_cancel_creation), {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }).show()
        }
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        if (isEditMode) {
            DialogHelper.makeYesNoDialogBuilder(this, "OmniTrack",
                    resources.getString(R.string.msg_confirm_trigger_apply_change),
                    {
                        if (validateConfigurations()) {
                            attachedTrigger?.let {
                                applyViewToTrigger(it)
                            }
                            super.onBackPressed()
                        } else {
                            DialogHelper.makeSimpleAlertBuilder(this, errorMessages.joinToString("\n")).show()
                        }
                    }, { setResult(Activity.RESULT_CANCELED); super.onBackPressed() }).show()
        } else {
            /*
            DialogHelper.makeYesNoDialogBuilder(this, "OmniTrack",
                    resources.getString(R.string.msg_confirm_trigger_cancel_creation), {
                super.onBackPressed()
            }).show()*/
            super.onBackPressed()
        }
    }

    private fun validateConfigurations(): Boolean {
        val configView = configurationCoordinatorView
        if (configView != null) {
            errorMessages.clear()
            return configView.validateConfigurations(errorMessages)
        } else {
            return true
        }
    }

    private fun applyViewToTrigger(trigger: OTTrigger): Boolean {
        configurationCoordinatorView?.applyConfigurationToTrigger(trigger)
        if (trackerAssignPanel != null) {
            for (trackerId in trackerAssignPanel!!.trackerIds)
                trigger.addTracker(trackerId)

            var i = 0
            while (i < trigger.trackers.size) {
                val tracker = trigger.trackers[i]
                if (!trackerAssignPanel!!.trackerIds.contains(tracker.objectId)) {
                    trigger.removeTracker(tracker)
                } else {
                    i++
                }
            }
        }
        return true
    }

    override fun onToolbarRightButtonClicked() {

        if (validateConfigurations()) {
            if (isEditMode) {
                attachedTrigger?.let {
                    if (applyViewToTrigger(it)) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }

            } else {
                val resultData = Intent()
                val newTrigger = OTTrigger.makeInstance(triggerType, "My Trigger", triggerAction, user!!)
                newTrigger.suspendDatabaseSync = true

                applyViewToTrigger(newTrigger)
                val pojo = newTrigger.dumpDataToPojo(null)
                println(pojo.properties)

                resultData.putExtra(INTENT_EXTRA_TRIGGER_DATA, pojo)
                setResult(RESULT_OK, resultData)
                finish()
            }
        } else {
            DialogHelper.makeSimpleAlertBuilder(this, errorMessages.joinToString("\n")).show()
        }
    }

    fun onUserLoaded(user: OTUser) {
        if (hideAttachedTrackers) {
            trackerAssignPanelContainer?.visibility = View.GONE
        } else {
            if (trackerAssignPanelContainer == null) {
                trackerAssignPanelContainer = trackerAssignPanelStub.inflate()
                trackerAssignPanel = trackerAssignPanelContainer?.findViewById(R.id.ui_tracker_assign_list) as TrackerAssignPanel
                trackerAssignPanel?.init(attachedTrigger?.trackers)
            } else {
                trackerAssignPanelContainer?.visibility = View.VISIBLE
            }
        }

        when (triggerType) {
            OTTrigger.TYPE_TIME -> {
                configurationCoordinatorView = TimeTriggerConfigurationPanel(this)
            }
            OTTrigger.TYPE_DATA_THRESHOLD -> {
                configurationCoordinatorView = EventTriggerConfigurationPanel(this)
            }
        }

        if (isEditMode)
            configurationCoordinatorView?.importTriggerConfiguration(attachedTrigger!!)

        if (configurationCoordinatorView != null)
            controlPanelContainer.addView(configurationCoordinatorView as? View)

    }

    override fun onStop() {
        super.onStop()
        startSubscriptions.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        user = null
        attachedTrigger = null
    }


}