package kr.ac.snu.hcil.omnitrack.utils

import android.content.Context
import android.support.v7.app.AlertDialog
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-07-13.
 */

class DialogHelper {
    companion object {
        fun makeYesNoDialogBuilder(context: Context, title: String, message: String, onYes: (() -> Unit)?, onNo: (() -> Unit)? = null): AlertDialog.Builder {
            val builder = AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(context.getText(R.string.msg_yes)){
                    dialog, which->
                        if(onYes!=null) onYes()
                }
                .setNegativeButton(context.getText(R.string.msg_no)){
                    dialog, which->
                        if(onNo!=null) onNo()
                }

            return builder
        }
    }
}