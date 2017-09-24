package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Dialog
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
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

        const val TAG = "FieldValueHistoryPicker"

        fun <T> getInstance(tracker: OTTracker, attribute: OTAttribute<T>): RecentItemValuePickerBottomSheetFragment {
            return getInstance(tracker.objectId, attribute.objectId)
        }

        fun getInstance(trackerId: String, attributeId: String): RecentItemValuePickerBottomSheetFragment {
            val arguments = Bundle()
            arguments.putString(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            arguments.putString(OTApplication.INTENT_EXTRA_OBJECT_ID_ATTRIBUTE, attributeId)

            return RecentItemValuePickerBottomSheetFragment().apply {
                this.arguments = arguments
            }
        }
    }

    private lateinit var viewContainer: ViewGroup
    private lateinit var listView: RecyclerView
    private lateinit var loadingIndicator: ProgressBar

    override fun setupDialogAndContentView(dialog: Dialog, contentView: View) {
        this.listView = contentView.findViewById(R.id.ui_list)
        this.loadingIndicator = contentView.findViewById(R.id.ui_loading_indicator)
        this.viewContainer = contentView.findViewById(R.id.ui_view_container)

    }
}