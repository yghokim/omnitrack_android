package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.ATriggerListFragmentCore
import rx.subscriptions.CompositeSubscription
import java.util.*

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

            //TODO remove this to unlock data-driven trigger
            override fun onNewTriggerButtonClicked() {
                val newTrigger = triggerAdapter?.makeNewTriggerInstance(OTTrigger.TYPE_TIME)
                if (newTrigger != null)
                    super.appendNewTrigger(newTrigger)
            }
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onTrackerLoaded(tracker: OTTracker) {

        startSubscriptions.add(
                tracker.colorObservable.subscribe {
                    color ->
                    core.setFloatingButtonColor(color)
                }
        )

        core.triggerAdapter = object : ATriggerListFragmentCore.TriggerAdapter {
            private var mTempTriggers: ArrayList<OTTrigger>? = null

            init {
                if (!isEditMode) {
                    mTempTriggers = ArrayList<OTTrigger>()
                }
            }

            val triggers: Array<OTTrigger> get() =
            //if(isEditMode){
            tracker.owner?.triggerManager?.getAttachedTriggers(tracker, OTTrigger.ACTION_NOTIFICATION)!!
            //}
            //else{
            //    mTempTriggers!!.toTypedArray()
            //}


            override fun getTriggerAt(position: Int): OTTrigger {
                return triggers[position]
            }

            override fun makeNewTriggerInstance(type: Int): OTTrigger {
                val newTrigger = OTTrigger.makeInstance(type, "My Trigger", OTTrigger.ACTION_NOTIFICATION, tracker.owner!!, tracker)
                //if(!isEditMode)
                //{
                //    newTrigger.switchDisconnected = true
                //}

                return newTrigger
            }

            override fun onAddTrigger(trigger: OTTrigger) {
                //if(isEditMode) {
                tracker.owner?.triggerManager?.putNewTrigger(trigger)
                //}
                //else{
                mTempTriggers?.add(trigger)
                //}
            }

            override fun onRemoveTrigger(trigger: OTTrigger) {
                //if(isEditMode) {
                tracker.owner?.triggerManager?.removeTrigger(trigger)
                //}
                //else{

                //    mTempTriggers?.remove(trigger)
                //}
            }

            override fun triggerCount(): Int {
                return triggers.size
            }

            override val withIndex: Iterable<IndexedValue<OTTrigger>>
                get() = triggers.withIndex()

        }
    }

    override fun onResume() {
        super.onResume()

        val activity = activity
        if (activity is TrackerDetailActivity) {

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