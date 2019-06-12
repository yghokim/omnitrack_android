package kr.ac.snu.hcil.android.common.view

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.layout_dialog_validation_text_input.view.*
import kotlinx.android.synthetic.main.layout_password_reset.view.*

/**
 * Created by Young-Ho Kim on 2016-07-13.
 */

object DialogHelper {

    class ChangePasswordWrongException(
            val originalPasswordErrorMessage: CharSequence? = null,
            val newPasswordErrorMessage: CharSequence? = null) : Exception()

    fun makeYesNoDialogBuilder(context: Context, title: String?, message: String, yesLabel: Int = R.string.msg_yes, noLabel: Int = R.string.msg_no, onYes: ((Dialog) -> Unit)?, onNo: ((Dialog) -> Unit)? = null, cancelable: Boolean = true, onCancel: ((Any) -> Unit)? = null): MaterialDialog.Builder {

        return MaterialDialog.Builder(context)
                .apply {
                    if (title != null) {
                        title(title)
                    }
                }
                .content(message)
                .positiveColorRes(R.color.colorPointed)
                .negativeColorRes(R.color.colorRed_Light)
                .positiveText(yesLabel)
                .negativeText(noLabel)
                .onPositive { materialDialog, dialogAction ->

                    if (onYes != null) onYes(materialDialog)
                }
                .onNegative { materialDialog, dialogAction ->

                    if (onNo != null) onNo(materialDialog)
                }
                .cancelable(cancelable)
                .cancelListener { dialog ->
                    if (onCancel != null) onCancel(dialog)
                }
    }

    fun makeNegativePhrasedYesNoDialogBuilder(context: Context, title: String?, message: String, yesLabel: Int = R.string.msg_yes, noLabel: Int = R.string.msg_no, onYes: ((Dialog) -> Unit)?, onNo: ((Dialog) -> Unit)? = null): MaterialDialog.Builder {
        return makeYesNoDialogBuilder(context, title, message, yesLabel, noLabel, onYes, onNo)
                .positiveColorRes(R.color.colorRed_Light)
                .negativeColorRes(R.color.colorPointed)
    }

    fun makeYesNoDialogBuilder(context: Context, title: String?, message: String, onYes: ((Dialog) -> Unit)?, onNo: ((Dialog) -> Unit)? = null): MaterialDialog.Builder {
        return makeYesNoDialogBuilder(context, title, message, R.string.msg_yes, R.string.msg_no, onYes, onNo)
    }

    fun makeSimpleAlertBuilder(context: Context, message: CharSequence, title: CharSequence?, @StringRes okLabelRes: Int? = null, onOk: (() -> Unit)? = null): MaterialDialog.Builder {
        return MaterialDialog.Builder(context)
                .apply {
                    if (title != null) {
                        this.title(title)
                    }
                }
                .content(message)
                .cancelable(false)
                .positiveColorRes(R.color.colorPointed)
                .positiveText(okLabelRes ?: R.string.msg_ok)
                .onPositive { materialDialog, dialogAction ->
                    if (onOk != null) onOk()
                }
    }

    fun makeValidationTextInputDialog(context: Context, title: String?, content: String?, hint: String?, validateFunc: ((String) -> String?)?, task: (String) -> Completable, onResult: ((String?, DialogAction) -> Unit)? = null): MaterialDialog {

        var taskSubscription: Disposable? = null

        val view = LayoutInflater.from(context).inflate(R.layout.layout_dialog_validation_text_input, null)
        view.ui_input_form.hint = hint

        if (content != null) {
            view.ui_content_text.visibility = View.VISIBLE
            view.ui_content_text.text = content
        } else {
            view.ui_content_text.visibility = View.GONE
        }


        val tryOk = { dialog: MaterialDialog ->
            val inputText = view.ui_input_text.text?.toString() ?: ""
            val validationErrorMessage = validateFunc?.invoke(inputText)
            if (validationErrorMessage != null) {
                //invalid
                view.ui_input_form.error = validationErrorMessage
                onResult?.invoke(null, DialogAction.POSITIVE)
                false
            } else {

                //to busy mode
                view.ui_input_form.visibility = View.GONE
                view.ui_loading_indicator.visibility = View.VISIBLE
                dialog.getActionButton(DialogAction.POSITIVE).isEnabled = false
                dialog.getActionButton(DialogAction.POSITIVE).alpha = InterfaceHelper.ALPHA_INACTIVE
                dialog.getActionButton(DialogAction.NEGATIVE).isEnabled = false
                dialog.getActionButton(DialogAction.NEGATIVE).alpha = InterfaceHelper.ALPHA_INACTIVE



                //valid
                taskSubscription = task(inputText).subscribe({
                    dialog.dismiss()
                    onResult?.invoke(inputText, DialogAction.POSITIVE)
                }, { err ->

                    view.ui_input_form.visibility = View.VISIBLE
                    view.ui_loading_indicator.visibility = View.GONE
                    dialog.getActionButton(DialogAction.POSITIVE).isEnabled = true
                    dialog.getActionButton(DialogAction.POSITIVE).alpha = InterfaceHelper.ALPHA_ORIGINAL
                    dialog.getActionButton(DialogAction.NEGATIVE).isEnabled = true
                    dialog.getActionButton(DialogAction.NEGATIVE).alpha = InterfaceHelper.ALPHA_ORIGINAL

                    view.ui_input_form.error = err.message
                })
                true
            }
        }

        val dialog = MaterialDialog.Builder(context)
                .apply {
                    if (title != null) {
                        title(title)
                    }
                }.autoDismiss(false)
                .cancelable(true)
                .positiveText(R.string.msg_ok)
                .positiveColorRes(R.color.colorPointed)
                .negativeText(R.string.msg_cancel)
                .negativeColorRes(R.color.colorAccent)
                .customView(view, false)
                .onPositive { dialog, which ->
                    tryOk(dialog)
                }
                .onNegative { dialog, which ->
                    dialog.dismiss()
                    onResult?.invoke(null, DialogAction.NEGATIVE)
                }
                .showListener {
                    view.ui_input_text.requestFocus()
                    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(view.ui_input_text, InputMethodManager.SHOW_IMPLICIT)
                }
                .dismissListener {
                    taskSubscription?.dispose()
                }.build()


        view.ui_input_text.setOnEditorActionListener { v, actionId, event ->
            !tryOk(dialog)
        }

        return dialog
    }

    fun makePasswordResetDialog(context: Context,
                                validateNewPasswordFunc: ((String) -> CharSequence?)?,
                                changePasswordFunc: ((String, String) -> Completable)
    ): MaterialDialog {
        val subscriptions = CompositeDisposable()

        val view = LayoutInflater.from(context).inflate(R.layout.layout_password_reset, null)

        val tryOk = { dialog: DialogInterface ->

            val originalPasswordInput = view.ui_original_password_text.text?.toString() ?: ""
            val newPasswordInput = view.ui_new_password_text.text?.toString() ?: ""
            val newConfirmPasswordInput = view.ui_new_password_confirm_text.text?.toString() ?: ""

            val originalPasswordInvalidMessage = if (originalPasswordInput.isBlank()) "You should insert the original password." else null

            val newPasswordInvalidMessage = validateNewPasswordFunc?.invoke(newPasswordInput)

            view.ui_original_password_form.error = originalPasswordInvalidMessage
            view.ui_new_password_form.error = newPasswordInvalidMessage


            view.ui_new_password_confirm_form.error = if (newPasswordInput != newConfirmPasswordInput) {
                "The two passwords must match each other."
            } else null

            if (newPasswordInvalidMessage == null
                    && originalPasswordInvalidMessage == null
                    && newPasswordInput == newConfirmPasswordInput) {
                subscriptions.add(
                        changePasswordFunc(originalPasswordInput, newPasswordInput).subscribe({
                            dialog.dismiss()
                        }, { err ->
                            if (err is ChangePasswordWrongException) {
                                view.ui_original_password_form.error = err.originalPasswordErrorMessage
                                view.ui_new_password_form.error = err.newPasswordErrorMessage
                            } else {
                                view.ui_new_password_form.error = err.message
                            }
                        })
                )
                true
            } else {
                false
            }
        }

        val dialog = MaterialDialog.Builder(context)
                .title("Reset Password")
                .autoDismiss(false)
                .cancelable(true)
                .positiveText(R.string.msg_apply)
                .positiveColorRes(R.color.colorPointed)
                .negativeText(R.string.msg_cancel)
                .negativeColorRes(R.color.colorAccent)
                .customView(view, false)
                .onPositive { dialog, which ->
                    tryOk(dialog)
                }
                .onNegative { dialog, which ->
                    dialog.dismiss()
                }
                .dismissListener {
                    subscriptions.dispose()
                }.build()


        view.ui_new_password_confirm_text.setOnEditorActionListener { v, actionId, event ->
            !tryOk(dialog)
        }

        return dialog

    }
}