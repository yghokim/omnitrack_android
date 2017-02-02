package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.ATriggerListFragmentCore
import rx.subscriptions.CompositeSubscription

/**
 * Created by younghokim on 16. 7. 30..
 */
class TrackerDetailReminderTabFragment : TrackerDetailActivity.ChildFragment() {

    val core: ATriggerListFragmentCore

    private var resumeSubscriptions = CompositeSubscription()

    init {
        core = object : ATriggerListFragmentCore(this@TrackerDetailReminderTabFragment) {
            override val triggerActionTypeName: Int = R.string.msg_text_reminder
            override val emptyMessageId: Int = R.string.msg_reminder_empty

            override fun getTriggers(): Array<OTTrigger> {
                if (trackerObjectId != null) {
                    val tracker = user?.get(trackerObjectId!!)
                    return if (tracker != null) {
                        user?.triggerManager?.getAttachedTriggers(tracker, OTTrigger.ACTION_NOTIFICATION) ?: emptyArray()
                    } else emptyArray()
                } else return emptyArray()
            }

            override fun makeNewTriggerInstance(type: Int): OTTrigger {
                val tracker = user?.get(trackerObjectId!!)
                return OTTrigger.makeInstance(type, "My Trigger", OTTrigger.ACTION_NOTIFICATION, user!!, tracker!!)
            }

            //TODO remove this to unlock data-driven trigger
            override fun onNewTriggerButtonClicked() {
                val newTrigger = makeNewTriggerInstance(OTTrigger.TYPE_TIME)
                super.appendNewTrigger(newTrigger)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        core.refresh()
    }

    override fun onResume() {
        super.onResume()
        val activity = activity
        if (activity is TrackerDetailActivity) {
            resumeSubscriptions.add(
                    activity.signedInUserObservable.subscribe {
                        user ->
                        if (trackerObjectId != null) {
                            val tracker = user[trackerObjectId!!]
                            resumeSubscriptions.add(
                                    tracker?.colorObservable?.subscribe {
                                        color ->
                                        core.setFloatingButtonColor(color)
                                    }
                            )
                        }
                    }
            )

            resumeSubscriptions.add(
                    activity.trackerColorOnUI.subscribe {
                        color ->
                        core.setFloatingButtonColor(color)
                    }
            )
        }
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
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        core.onViewCreated(view, savedInstanceState)
    }

}