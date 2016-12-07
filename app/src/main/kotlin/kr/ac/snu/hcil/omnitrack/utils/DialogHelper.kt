package kr.ac.snu.hcil.omnitrack.utils

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-07-13.
 */

object DialogHelper {
    fun makeYesNoDialogBuilder(context: Context, title: String, message: String, onYes: (() -> Unit)?, onNo: (() -> Unit)? = null): MaterialDialog.Builder {
        val builder = MaterialDialog.Builder(context)
                .title(title)
                .content(message)
                .cancelable(true)
                .positiveColorRes(R.color.colorPointed)
                .negativeColorRes(R.color.colorRed_Light)
                .positiveText(R.string.msg_yes)
                .negativeText(R.string.msg_no)
                .onPositive { materialDialog, dialogAction ->

                        if (onYes != null) onYes()
                }
                .onNegative { materialDialog, dialogAction ->

                        if (onNo != null) onNo()
                    }

            return builder
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