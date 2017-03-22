package kr.ac.snu.hcil.omnitrack.utils

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-07-13.
 */

object DialogHelper {
    fun makeYesNoDialogBuilder(context: Context, title: String, message: String, yesLabel: Int = R.string.msg_yes, noLabel: Int = R.string.msg_no, onYes: (() -> Unit)?, onNo: (() -> Unit)? = null, cancelable: Boolean = true, onCancel: (() -> Unit)? = null): MaterialDialog.Builder {
        val builder = MaterialDialog.Builder(context)
                .title(title)
                .content(message)
                .positiveColorRes(R.color.colorPointed)
                .negativeColorRes(R.color.colorRed_Light)
                .positiveText(yesLabel)
                .negativeText(noLabel)
                .onPositive { materialDialog, dialogAction ->

                    if (onYes != null) onYes()
                }
                .onNegative { materialDialog, dialogAction ->

                    if (onNo != null) onNo()
                }
                .cancelable(cancelable)
                .cancelListener {
                    if (onCancel != null) onCancel()
                }

        return builder
    }

    fun makeNegativePhrasedYesNoDialogBuilder(context: Context, title: String, message: String, yesLabel: Int = R.string.msg_yes, noLabel: Int = R.string.msg_no, onYes: (() -> Unit)?, onNo: (() -> Unit)? = null): MaterialDialog.Builder {
        return makeYesNoDialogBuilder(context, title, message, yesLabel, noLabel, onYes, onNo)
                .positiveColorRes(R.color.colorRed_Light)
                .negativeColorRes(R.color.colorPointed)
    }

    fun makeYesNoDialogBuilder(context: Context, title: String, message: String, onYes: (() -> Unit)?, onNo: (() -> Unit)? = null): MaterialDialog.Builder {
        return makeYesNoDialogBuilder(context, title, message, R.string.msg_yes, R.string.msg_no, onYes, onNo)
    }

    fun makeSimpleAlertBuilder(context: Context, message: String, onOk: (() -> Unit)? = null): MaterialDialog.Builder {
        return MaterialDialog.Builder(context)
                .title("OmniTrack")
                .content(message)
                .cancelable(false)
                .positiveColorRes(R.color.colorPointed)
                .positiveText(R.string.msg_ok)
                .onPositive { materialDialog, dialogAction ->
                    if (onOk != null) onOk()
                }
    }
}