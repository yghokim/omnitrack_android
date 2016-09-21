package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.ATriggerListFragmentCore

/**
 * Created by Young-Ho on 9/1/2016.
 */
class LoggingTriggerListFragment : Fragment() {
    val core: ATriggerListFragmentCore

    init {
        core = object : ATriggerListFragmentCore(this) {
            override val triggerActionTypeName: Int = R.string.msg_text_trigger

            override fun getTriggers(): Array<OTTrigger> {
                return OTApplication.app.triggerManager.getTriggersOfAction(OTTrigger.ACTION_BACKGROUND_LOGGING)
            }

            override fun makeNewTriggerInstance(type: Int): OTTrigger {
                return OTTrigger.makeInstance(type, "My Trigger", OTTrigger.ACTION_BACKGROUND_LOGGING)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = core.onCreateView(inflater, container, savedInstanceState)

        return rootView
    }
}