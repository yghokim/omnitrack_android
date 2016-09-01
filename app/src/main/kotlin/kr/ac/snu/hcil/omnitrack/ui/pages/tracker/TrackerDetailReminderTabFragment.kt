package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.ATriggerListFragmentCore

/**
 * Created by younghokim on 16. 7. 30..
 */
class TrackerDetailReminderTabFragment : TrackerDetailActivity.ChildFragment() {

    val core: ATriggerListFragmentCore

    init {
        core = object : ATriggerListFragmentCore(this) {
            override fun getTriggers(): Array<OTTrigger> {
                return OTApplication.app.triggerManager.getAttachedTriggers(tracker, OTTrigger.ACTION_NOTIFICATION)
            }

            override fun makeNewTriggerInstance(type: Int): OTTrigger {
                return OTTrigger.makeInstance(type, "My Trigger", OTTrigger.ACTION_NOTIFICATION, tracker)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = core.onCreateView(inflater, container, savedInstanceState)

        return rootView
    }

}