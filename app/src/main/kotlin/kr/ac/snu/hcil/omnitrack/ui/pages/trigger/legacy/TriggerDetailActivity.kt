package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.legacy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.FrameLayout
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.activities.OTFragment
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.EventTriggerConfigurationPanel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.ITriggerConfigurationCoordinator
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.TrackerAssignPanel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.actions.NotificationSettingsPanelView
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import java.util.*

/**
 * Created by younghokim on 2017. 2. 21..
 */
class TriggerDetailActivity : MultiButtonActionBarActivity(R.layout.activity_multibutton_single_fragment) {

    companion object {
        const val INTENT_EXTRA_TRIGGER_TYPE = "trigger_type"
        const val INTENT_EXTRA_TRIGGER_ACTION = "trigger_action"
        const val INTENT_EXTRA_HIDE_ATTACHED_TRACKERS = "hide_attached_trackers"
        const val INTENT_EXTRA_TRIGGER_DATA = "trigger_data"
        const val INTENT_EXTRA_OVERRIDE_TITLE = "overrideTitle"

        fun makeNewTriggerIntent(context: Context, triggerType: Int, triggerAction: Int, hideAttachedTrackers: Boolean = false, overrideTitle: String? = null): Intent {
            val intent = Intent(context, TriggerDetailActivity::class.java)
                    .putExtra(INTENT_EXTRA_TRIGGER_TYPE, triggerType)
                    .putExtra(INTENT_EXTRA_TRIGGER_ACTION, triggerAction)
                    .putExtra(INTENT_EXTRA_HIDE_ATTACHED_TRACKERS, hideAttachedTrackers)

            if (overrideTitle != null) {
                intent.putExtra(INTENT_EXTRA_OVERRIDE_TITLE, overrideTitle)
            }

            return intent
        }

        fun makeEditTriggerIntent(context: Context, triggerId: String, hideAttachedTrackers: Boolean = false, overrideTitle: String? = null): Intent {
            val intent = Intent(context, TriggerDetailActivity::class.java)
                    .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER, triggerId)
                    .putExtra(INTENT_EXTRA_HIDE_ATTACHED_TRACKERS, hideAttachedTrackers)

            if (overrideTitle != null) {
                intent.putExtra(INTENT_EXTRA_OVERRIDE_TITLE, overrideTitle)
            }

            return intent
        }
    }

    private var contentFragment: TriggerDetailFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setActionBarButtonMode(Mode.SaveCancel)

        var hideAttachedTrackers: Boolean = false
        if (intent.hasExtra(INTENT_EXTRA_HIDE_ATTACHED_TRACKERS)) {
            hideAttachedTrackers = intent.getBooleanExtra(INTENT_EXTRA_HIDE_ATTACHED_TRACKERS, false)
        }

        val triggerId = intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER)
        if (!triggerId.isNullOrBlank()) {

            title = resources.getString(R.string.title_activity_trigger_edit)
            setActionBarButtonMode(Mode.ApplyCancel)

        } else {
            title = resources.getString(R.string.title_activity_trigger_new)
        }

        val overrideTitle = intent.getStringExtra(INTENT_EXTRA_OVERRIDE_TITLE)
        if (!overrideTitle.isNullOrBlank()) {
            title = overrideTitle
        }

        val frag = supportFragmentManager.findFragmentByTag("TriggerDetailContent") as? TriggerDetailFragment
        if (frag != null) {
            contentFragment = frag
        } else {
            contentFragment = if (!triggerId.isNullOrBlank()) {
                TriggerDetailFragment.getInstance(triggerId, hideAttachedTrackers)
            } else {
                val triggerType = intent.getIntExtra(INTENT_EXTRA_TRIGGER_TYPE, -1)
                val triggerAction = intent.getIntExtra(INTENT_EXTRA_TRIGGER_ACTION, -1)
                TriggerDetailFragment.getInstance(triggerType, triggerAction, hideAttachedTrackers)
            }

            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.ui_content_replace, contentFragment, "TriggerDetailContent")
            transaction.commit()
        }


    }

    override fun onToolbarLeftButtonClicked() {
        if (contentFragment?.isEditMode == true) {
            DialogHelper.makeYesNoDialogBuilder(this, "OmniTrack",
                    resources.getString(R.string.msg_confirm_trigger_apply_change), R.string.msg_apply, onYes =
            {
                val errorMessages = contentFragment?.validateAndApplyViewStateToTrigger()
                if (errorMessages == null) {

                    setResult(Activity.RESULT_OK)
                    finish()
                } else {

                    DialogHelper.makeSimpleAlertBuilder(this, errorMessages.joinToString("\n")).show()
                }
            }, onNo = { setResult(Activity.RESULT_CANCELED); finish() }).show()
        } else {
            DialogHelper.makeNegativePhrasedYesNoDialogBuilder(this, "OmniTrack",
                    resources.getString(R.string.msg_confirm_trigger_cancel_creation), R.string.msg_cancel_creation_and_exit, onYes = {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }).show()
        }
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        if (contentFragment?.isEditMode == true) {
            DialogHelper.makeYesNoDialogBuilder(this, "OmniTrack",
                    resources.getString(R.string.msg_confirm_trigger_apply_change), R.string.msg_apply, onYes = {

                val errorMessages = contentFragment?.validateAndApplyViewStateToTrigger()
                if (errorMessages != null) {
                    DialogHelper.makeSimpleAlertBuilder(this, errorMessages.joinToString("\n")).show()
                } else {
                    super.onBackPressed()
                }
            }, onNo = { setResult(Activity.RESULT_CANCELED); super.onBackPressed() }).show()
        } else {
            /*
            DialogHelper.makeYesNoDialogBuilder(this, "OmniTrack",
                    resources.getString(R.string.msg_confirm_trigger_cancel_creation), {
                super.onBackPressed()
            }).show()*/
            super.onBackPressed()
        }
    }


    override fun onToolbarRightButtonClicked() {

        if (contentFragment?.validateConfigurations() == true) {
            if (contentFragment?.isEditMode == true) {
                contentFragment?.attachedTrigger?.let {
                    if (contentFragment?.applyViewToTrigger(it) == true) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }

            } else {
                contentFragment?.let { contentFragment ->
                    val resultData = Intent()
                    val newTrigger = OTTrigger.makeInstance(contentFragment.triggerType, "My Trigger", contentFragment.triggerAction, contentFragment.user!!)
                    newTrigger.suspendDatabaseSync = true

                    contentFragment.applyViewToTrigger(newTrigger)
                    /*
                    val pojo = newTrigger.dumpDataToPojo(null)
                    println(pojo.properties)

                    resultData.putExtra(INTENT_EXTRA_TRIGGER_DATA, pojo)*/
                    setResult(RESULT_OK, resultData)
                    finish()
                } ?: finish()

            }
        } else {
            contentFragment?.let {
                DialogHelper.makeSimpleAlertBuilder(this, it.errorMessages.joinToString("\n")).show()
            }
        }
    }

    class TriggerDetailFragment : OTFragment() {
        companion object {
            fun getInstance(triggerId: String, hideAttachedTrackers: Boolean): TriggerDetailFragment {
                val args = Bundle()
                args.putString(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER, triggerId)
                args.putBoolean(INTENT_EXTRA_HIDE_ATTACHED_TRACKERS, hideAttachedTrackers)

                val fragment = TriggerDetailFragment()
                fragment.arguments = args
                return fragment
            }

            fun getInstance(triggerType: Int, triggerAction: Int, hideAttachedTrackers: Boolean): TriggerDetailFragment {
                val args = Bundle()
                args.putInt(INTENT_EXTRA_TRIGGER_ACTION, triggerAction)
                args.putInt(INTENT_EXTRA_TRIGGER_TYPE, triggerType)
                args.putBoolean(INTENT_EXTRA_HIDE_ATTACHED_TRACKERS, hideAttachedTrackers)

                val fragment = TriggerDetailFragment()
                fragment.arguments = args
                return fragment
            }


        }


        private var triggerId: String? = null
        var attachedTrigger: OTTrigger? = null
            private set

        var triggerType: Int = -1
            private set

        var triggerAction: Int = -1
            private set

        var user: OTUser? = null
            private set

        val isEditMode: Boolean get() = triggerId != null

        private var actionSettingsView: ITriggerConfigurationCoordinator? = null

        private var configurationCoordinatorView: ITriggerConfigurationCoordinator? = null

        private lateinit var controlPanelContainer: ViewGroup
        private lateinit var trackerAssignPanelStub: ViewStub

        private lateinit var actionSettingsContainer: ViewGroup

        private var trackerAssignPanelContainer: View? = null
        private var trackerAssignPanel: TrackerAssignPanel? = null

        private var hideAttachedTrackers: Boolean = false

        val errorMessages = ArrayList<String>()

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

            println("create view")

            val view = inflater.inflate(R.layout.activity_trigger_detail, container, false)
            controlPanelContainer = view.findViewById(R.id.ui_condition_control_panel_container)
            trackerAssignPanelStub = view.findViewById(R.id.ui_tracker_assign_panel_stub)
            actionSettingsContainer = view.findViewById(R.id.ui_action_settings_container)

            this.hideAttachedTrackers = arguments.getBoolean(INTENT_EXTRA_HIDE_ATTACHED_TRACKERS, false)

            val triggerId = arguments.getString(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER)
            if (!triggerId.isNullOrBlank()) {
                val activity = activity
                /*
                if (activity is OTActivity) {
                    createViewSubscriptions.add(
                            activity.signedInUserObservable.doOnNext { user -> this.user = user }.flatMap {
                                user ->
                                user.getTriggerObservable(triggerId)
                            }.doOnNext {
                                trigger ->
                                attachedTrigger = trigger
                                this.triggerId = trigger.objectId
                            }.subscribe {
                                trigger ->
                                triggerConditionType = trigger.typeId
                                triggerActionType = trigger.action
                                //attachedTrigger?.dumpDataToPojo(null)?.toMutable(currentTriggerPojo)

                                onUserLoaded(savedInstanceState)
                            })
                }*/
            } else {
                /*
                triggerConditionType = arguments.getInt(INTENT_EXTRA_TRIGGER_TYPE, -1)
                triggerActionType = arguments.getInt(INTENT_EXTRA_TRIGGER_ACTION, -1)
                val activity = activity
                if (activity is OTActivity) {
                    createViewSubscriptions.add(
                            activity.signedInUserObservable.doOnNext { user -> this.user = user }.subscribe {
                                onUserLoaded(savedInstanceState)
                            }
                    )
                }*/
            }

            return view
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            println("create trigger detail content fragment")

            retainInstance = true
        }

        override fun onSaveInstanceState(outState: Bundle?) {
            super.onSaveInstanceState(outState)
            if (outState != null) {
                configurationCoordinatorView?.writeConfigurationToBundle(outState)
                if (!hideAttachedTrackers) {
                    outState.putStringArrayList("assignedTrackers", trackerAssignPanel?.trackerIds)
                }
            }
        }

        private fun setupTriggerActionPanel(triggerAction: Int, trigger: OTTrigger?) {
            val view = when (triggerAction) {
                OTTrigger.ACTION_BACKGROUND_LOGGING -> {
                    null
                }
                OTTrigger.ACTION_NOTIFICATION -> {
                    actionSettingsView = actionSettingsView as? NotificationSettingsPanelView ?:
                            NotificationSettingsPanelView(context).apply {
                                this.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
                            }

                    actionSettingsView as? View
                }
                else -> null
            }

            if (view != null) {
                if (isEditMode)
                    (view as? ITriggerConfigurationCoordinator)?.importTriggerConfiguration(attachedTrigger!!)

                if (view.parent !== actionSettingsContainer) {
                    if (view.parent != null) {
                        (view.parent as ViewGroup).removeView(view)
                    }

                    actionSettingsContainer.addView(view)
                }
            }
        }

        fun validateAndApplyViewStateToTrigger(): List<String>? {
            if (validateConfigurations()) {
                attachedTrigger?.let {
                    applyViewToTrigger(it)
                }
                return null
            } else return errorMessages
        }

        fun onUserLoaded(savedInstanceState: Bundle?) {
            if (hideAttachedTrackers) {
                trackerAssignPanelContainer?.visibility = View.GONE
            } else {
                if (trackerAssignPanelContainer == null) {
                    trackerAssignPanelContainer = trackerAssignPanelStub.inflate()
                    trackerAssignPanel = trackerAssignPanelContainer?.findViewById(R.id.ui_tracker_assign_list)
                    trackerAssignPanel?.init(attachedTrigger?.trackers)
                } else {
                    trackerAssignPanelContainer?.visibility = View.VISIBLE
                }
            }

            when (triggerType) {
                OTTrigger.TYPE_TIME -> {
                    //configurationCoordinatorView = TimeTriggerConfigurationPanel(this.context)
                }
                OTTrigger.TYPE_DATA_THRESHOLD -> {
                    configurationCoordinatorView = EventTriggerConfigurationPanel(this.context)
                }
            }

            setupTriggerActionPanel(triggerAction, attachedTrigger)

            if (isEditMode)
                configurationCoordinatorView?.importTriggerConfiguration(attachedTrigger!!)

            if (configurationCoordinatorView != null) {
                controlPanelContainer.addView(configurationCoordinatorView as? View)
            }

            if (savedInstanceState != null) {
                if (!hideAttachedTrackers) {
                    /*
                    val trackers = savedInstanceState.getStringArrayList("assignedTrackers")
                    if (trackers != null) {
                        val activity = activity
                        if (activity is OTActivity) {
                            createViewSubscriptions.add(
                                    activity.signedInUserObservable.flatMap {
                                        user ->
                                        user.crawlAllTrackersAndTriggerAtOnce().toObservable()
                                    }.subscribe { user ->
                                        trackerAssignPanel?.init(trackers.map { it -> user[it] }.filter { it != null }.map { it as OTTracker })
                                    }
                            )
                        }
                    }*/
                }

                configurationCoordinatorView?.readConfigurationFromBundle(savedInstanceState)
            }

        }

        fun validateConfigurations(): Boolean {
            val configView = configurationCoordinatorView
            if (configView != null) {
                errorMessages.clear()
                return configView.validateConfigurations(errorMessages)
            } else {
                return true
            }
        }

        fun applyViewToTrigger(trigger: OTTrigger): Boolean {
            configurationCoordinatorView?.applyConfigurationToTrigger(trigger)
            if (trackerAssignPanel != null) {
                for (trackerId in trackerAssignPanel!!.trackerIds) {//trigger.addTracker(trackerId)
                }
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

            actionSettingsView?.applyConfigurationToTrigger(trigger)
            return true
        }

        override fun onDestroyView() {
            super.onDestroyView()
            createViewSubscriptions.clear()
        }
    }


}