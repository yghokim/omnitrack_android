package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.View
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.components.common.DismissingBottomSheetDialogFragment

/**
 * Created by younghokim on 2017. 9. 21..
 */
class RecentItemValuePickerBottomSheetFragment: DismissingBottomSheetDialogFragment(R.layout.fragment_field_value_history_list) {

    interface Callback<T>{
        fun onValuePicked(value: T)
    }

    companion object {
        fun <T> getInstance(tracker: OTTracker, attribute: OTAttribute<T>): BottomSheetDialogFragment {
            val arguments = Bundle()
            arguments.putString(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
            arguments.putString(OTApplication.INTENT_EXTRA_OBJECT_ID_ATTRIBUTE, attribute.objectId)

            return RecentItemValuePickerBottomSheetFragment().apply{
                this.arguments = arguments
            }
        }
    }

    override fun setupDialogAndContentView(dialog: Dialog, contentView: View) {

    }
}