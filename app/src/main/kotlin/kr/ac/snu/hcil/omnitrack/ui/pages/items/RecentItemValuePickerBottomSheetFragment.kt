package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import kr.ac.snu.hcil.android.common.view.dialog.DismissingBottomSheetDialogFragment
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by younghokim on 2017. 9. 21..
 */
class RecentItemValuePickerBottomSheetFragment: DismissingBottomSheetDialogFragment(R.layout.fragment_field_value_history_list) {

    interface Callback<T>{
        fun onValuePicked(value: T)
    }

    companion object {

        const val TAG = "FieldValueHistoryPicker"

        fun getInstance(trackerId: String, attributeId: String): RecentItemValuePickerBottomSheetFragment {
            val arguments = Bundle()
            arguments.putString(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            arguments.putString(OTApp.INTENT_EXTRA_OBJECT_ID_ATTRIBUTE, attributeId)

            return RecentItemValuePickerBottomSheetFragment().apply {
                this.arguments = arguments
            }
        }
    }

    private lateinit var viewContainer: ViewGroup
    private lateinit var listView: RecyclerView
    private lateinit var loadingIndicator: ProgressBar

    override fun setupDialogAndContentView(dialog: Dialog, contentView: View) {
        this.listView = contentView.findViewById(R.id.ui_recyclerview_with_fallback)
        this.loadingIndicator = contentView.findViewById(R.id.ui_loading_indicator)
        this.viewContainer = contentView.findViewById(R.id.ui_view_container)

    }
}