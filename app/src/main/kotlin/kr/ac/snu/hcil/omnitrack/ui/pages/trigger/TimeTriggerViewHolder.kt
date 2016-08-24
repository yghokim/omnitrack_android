package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.view.View
import android.view.ViewGroup
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTimeTrigger

/**
 * Created by younghokim on 16. 8. 24..
 */
class TimeTriggerViewHolder : ATriggerViewHolder<OTTimeTrigger> {

    constructor(parent: ViewGroup, listener: ITriggerControlListener, context: Context) : super(parent, listener, context)

    override fun getConfigSummary(trigger: OTTimeTrigger): CharSequence {
        if (trigger.isConfigSpecified) {

        }

        if (trigger.isRangeSpecified) {

        }

        return ""
    }

    override fun initExpandedViewContent(): View {
        return View(itemView.context)
    }

    override fun updateExpandedViewContent(trigger: OTTimeTrigger) {

    }

}