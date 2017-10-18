package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.disposables.CompositeDisposable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.activities.OTFragment
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.ATriggerListFragmentCore
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.TriggerDetailActivity

/**
 * Created by younghokim on 16. 7. 30..
 */
class TrackerDetailReminderTabFragment : OTFragment() {

    val core = Core()

    private lateinit var viewModel: TrackerDetailViewModel

    private var creationSubscriptions = CompositeDisposable()

    private var resumeSubscriptions = CompositeDisposable()

    init {
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        this.viewModel = ViewModelProviders.of(activity).get(TrackerDetailViewModel::class.java)

        creationSubscriptions.add(
                this.viewModel.colorObservable.subscribe { color ->
                    core.setFloatingButtonColor(color)
                }
        )

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        core.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        core.onDestroy()
    }


    override fun onPause() {
        super.onPause()
        resumeSubscriptions.clear()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        core.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = core.onCreateView(inflater, container, savedInstanceState)
        return rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        core.onDestroyView()
        creationSubscriptions.clear()
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        core.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        core.onActivityResult(requestCode, resultCode, data)
    }

    inner class Core : ATriggerListFragmentCore(this@TrackerDetailReminderTabFragment) {

        var trackerId: String? = null

        override val triggerFilter: (OTTrigger) -> Boolean = { trigger ->
            trigger.action == OTTrigger.ACTION_NOTIFICATION &&
                    trigger.trackers.find { it.objectId == trackerId } != null
        }

        override val triggerActionType = OTTrigger.ACTION_NOTIFICATION
        override val triggerActionTypeName: Int = R.string.msg_text_reminder
        override val emptyMessageId: Int = R.string.msg_reminder_empty

        //TODO remove this to unlock data-driven trigger
        override fun onNewTriggerButtonClicked() {
            /*
            val newTrigger = OTTrigger.makeInstance(OTTrigger.TYPE_TIME, "My Trigger", OTTrigger.ACTION_NOTIFICATION,)
            if (newTrigger != null)
                super.appendNewTrigger(newTrigger)*/
            this@TrackerDetailReminderTabFragment
                    .startActivityForResult(
                            TriggerDetailActivity.makeNewTriggerIntent(parent.context, OTTrigger.TYPE_TIME, triggerActionType, hideTrackerAssignmentInterface(), resources.getString(R.string.title_activity_reminder_new)), DETAIL_REQUEST_CODE)

        }

        override fun postProcessNewlyAddedTrigger(newTrigger: OTTrigger) {
            super.postProcessNewlyAddedTrigger(newTrigger)
            trackerId?.let {
                newTrigger.addTracker(it)
            }
        }

        override fun hideTrackerAssignmentInterface(): Boolean {
            return true
        }

        override fun onTriggerEditRequested(triggerId: String) {

            parent.startActivityForResult(
                    TriggerDetailActivity.makeEditTriggerIntent(parent.context, triggerId, hideTrackerAssignmentInterface(), resources.getString(R.string.title_activity_reminder_edit)),
                    DETAIL_REQUEST_CODE
            )
        }
    }

}