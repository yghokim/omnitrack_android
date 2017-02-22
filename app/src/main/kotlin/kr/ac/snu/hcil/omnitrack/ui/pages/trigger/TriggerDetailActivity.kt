package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

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

/**
 * Created by younghokim on 2017. 2. 21..
 */
class TriggerDetailActivity : MultiButtonActionBarActivity(R.layout.activity_trigger_detail) {

    companion object {
        const val INTENT_EXTRA_TRIGGER_TYPE = "trigger_type"
        const val INTENT_EXTRA_TRIGGER_ACTION = "trigger_action"
        const val INTENT_EXTRA_HIDE_ATTACHED_TRACKERS = "hide_attached_trackers"

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

    private var user: OTUser? = null

    private val isEditMode: Boolean get() = triggerId != null

    private val controlPanelContainer: ViewGroup by bindView(R.id.ui_control_panel)
    private val trackerAssignPanelStub: ViewStub by bindView(R.id.ui_tracker_assign_panel_stub)
    private var trackerAssignPanelContainer: View? = null
    private var trackerAssignPanel: TrackerAssignPanel? = null

    private var hideAttachedTrackers: Boolean = false

    private var startSubscriptions = SubscriptionList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActionBarButtonMode(Mode.SaveCancel)
    }

    override fun onToolbarLeftButtonClicked() {
        if (isEditMode) {
            DialogHelper.makeYesNoDialogBuilder(this, "OmniTrack", resources.getString(R.string.msg_confirm_trigger_apply_change), {
                applyViewToTrigger()
                finish()
            }, { finish() }).show()
        } else {
            DialogHelper.makeYesNoDialogBuilder(this, "OmniTrack", resources.getString(R.string.msg_confirm_trigger_cancel_creation), {
                applyViewToTrigger()
                finish()
            }, { finish() }).show()
        }
    }

    private fun applyViewToTrigger() {

    }

    override fun onToolbarRightButtonClicked() {

    }

    override fun onStart() {
        super.onStart()

        if (intent.hasExtra(INTENT_EXTRA_HIDE_ATTACHED_TRACKERS)) {
            hideAttachedTrackers = intent.getBooleanExtra(INTENT_EXTRA_HIDE_ATTACHED_TRACKERS, false)
        }

        startSubscriptions.add(
                signedInUserObservable.subscribe {
                    user ->
                    this.user = user
                    if (intent.hasExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRIGGER)) {
                        val triggerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRIGGER)
                        attachedTrigger = user.triggerManager.getTriggerWithId(triggerId)
                        this.triggerId = triggerId

                        //attachedTrigger?.dumpDataToPojo(null)?.toMutable(currentTriggerPojo)

                        title = resources.getString(R.string.title_activity_trigger_edit)
                        setActionBarButtonMode(Mode.ApplyCancel)
                    } else {
                        title = resources.getString(R.string.title_activity_trigger_new)
                        setActionBarButtonMode(Mode.SaveCancel)
                    }

                    onUserLoaded(user)
                }
        )
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
    }

    override fun onStop() {
        super.onStop()
        user = null
        attachedTrigger = null
        startSubscriptions.clear()
    }


}