package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialogFragment
import android.support.design.widget.CoordinatorLayout
import android.view.View

/**
 * Created by younghokim on 2017. 9. 23..
 */
abstract class DismissingBottomSheetDialogFragment(private val dialogLayoutId: Int) : BottomSheetDialogFragment() {
    private val behaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {

        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss()
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
            behavior.setBottomSheetCallback(behaviorCallback)
        }

        setupDialogAndContentView(dialog, contentView)
    }

    protected abstract fun setupDialogAndContentView(dialog: Dialog, contentView: View)
}