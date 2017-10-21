package kr.ac.snu.hcil.omnitrack.ui.pages.home.legacy

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.activities.OTFragment
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.ATriggerListFragmentCore

/**
 * Created by Young-Ho on 9/1/2016.
 */
class LoggingTriggerListFragment : OTFragment() {
    val core: ATriggerListFragmentCore

    init {
        core = object : ATriggerListFragmentCore(this@LoggingTriggerListFragment) {
            override val triggerFilter: (OTTrigger) -> Boolean = { trigger -> trigger.action == OTTrigger.ACTION_BACKGROUND_LOGGING }

            override val triggerActionType = OTTrigger.ACTION_BACKGROUND_LOGGING
            override val triggerActionTypeName: Int = R.string.msg_text_trigger
            override val emptyMessageId: Int = R.string.msg_trigger_empty

            /*
            override fun getTriggers(): Array<OTTrigger> {
                return user?.triggerManager?.getTriggersOfAction(OTTrigger.ACTION_BACKGROUND_LOGGING) ?: emptyArray()
            }

            override fun makeNewTriggerInstance(type: Int): OTTrigger {
                return OTTrigger.makeInstance(type, "My Trigger", OTTrigger.ACTION_BACKGROUND_LOGGING, user!!)
            }*/
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        core.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        core.onDestroy()
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        core.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        core.onActivityResult(requestCode, resultCode, data)
    }
}