package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Created by younghokim on 2017. 9. 23..
 */
abstract class DismissingBottomSheetDialogFragment(private val dialogLayoutId: Int) : BottomSheetDialogFragment() {
    class DismissOnHiddenCallback(val fragment: DialogFragment) : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {

        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                fragment.dismiss()
            }
        }

    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val contentView = View.inflate(context, dialogLayoutId, null)
        dialog.setContentView(contentView)
        val lp = ((contentView.parent as View).layoutParams as CoordinatorLayout.LayoutParams)
        val behavior = lp.behavior
        if (behavior is BottomSheetBehavior) {
            behavior.setBottomSheetCallback(DismissOnHiddenCallback(this))
        }

        setupDialogAndContentView(dialog, contentView)
    }

    protected abstract fun setupDialogAndContentView(dialog: Dialog, contentView: View)
}