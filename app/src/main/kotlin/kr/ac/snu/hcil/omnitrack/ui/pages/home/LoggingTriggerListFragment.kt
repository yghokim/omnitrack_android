package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.ui.activities.OTFragment
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.ATriggerListFragmentCore
import rx.subscriptions.CompositeSubscription

/**
 * Created by Young-Ho on 9/1/2016.
 */
class LoggingTriggerListFragment : OTFragment() {
    val core: ATriggerListFragmentCore

    private val startSubscriptions = CompositeSubscription()

    init {
        core = object : ATriggerListFragmentCore(this@LoggingTriggerListFragment) {

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

    override fun onStart() {
        super.onStart()
        val activity = activity
        if (activity is OTActivity) {
            activity.signedInUserObservable.subscribe {
                user ->
                core.triggerAdapter = object : ATriggerListFragmentCore.TriggerAdapter {
                    val triggers: Array<OTTrigger> get() = user.triggerManager.getTriggersOfAction((OTTrigger.ACTION_BACKGROUND_LOGGING))
                    override fun getTriggerAt(position: Int): OTTrigger {
                        return triggers[position]
                    }

                    override fun makeNewTriggerInstance(type: Int): OTTrigger {
                        return OTTrigger.makeInstance(type, "My Trigger", OTTrigger.ACTION_BACKGROUND_LOGGING, user)
                    }

                    override fun onAddTrigger(trigger: OTTrigger) {
                        user.triggerManager.putNewTrigger(trigger)
                    }

                    override fun onRemoveTrigger(trigger: OTTrigger) {
                        user.triggerManager.removeTrigger(trigger)
                    }

                    override fun triggerCount(): Int {
                        return triggers.size
                    }

                    override val withIndex: Iterable<IndexedValue<OTTrigger>>
                        get() = triggers.withIndex()

                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        startSubscriptions.clear()
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
}