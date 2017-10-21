package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.view.View
import android.view.ViewGroup
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.legacy.DataTriggerViewModel

/**
 * Created by younghokim on 16. 9. 5..
 */
class EventTriggerViewHolder(parent: ViewGroup, listener: ITriggerControlListener, context: Context) : ATriggerViewHolder<DataTriggerViewModel>(parent, listener, context) {


    override fun validateTriggerSwitchOn(): Boolean {
        //return super.validateTriggerSwitchOn() && trigger.measure != null && trigger.conditioner != null
        return false
    }

    override fun getHeaderView(current: View?, viewModel: DataTriggerViewModel): View {

        val view = current as? EventTriggerDisplayView ?: EventTriggerDisplayView(itemView.context)
        //view.setConditioner(trigger.conditioner as? SingleNumericComparison)
        //view.setMeasureFactory(trigger.measure?.factory)

        return view
    }

    override fun getViewsForSwitchValidationFailedAlert(): Array<View>? {
        /*
        val superValue = super.getViewsForSwitchValidationFailedAlert()

        val childValue = if (trigger.measure == null || trigger.conditioner == null) {
            arrayOf(headerViewContainer as View)
        } else null

        return if (superValue != null && childValue != null) superValue + childValue
        else if (superValue != null && childValue == null) {
            superValue
        } else if (childValue != null && superValue == null) {
            childValue
        } else null*/
        return null
    }
}