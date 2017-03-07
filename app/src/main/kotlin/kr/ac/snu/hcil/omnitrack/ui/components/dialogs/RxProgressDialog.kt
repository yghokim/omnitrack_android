package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.app.ProgressDialog
import android.content.Context
import rx.Observable
import rx.Single

/**
 * Created by Young-Ho Kim on 2017-03-07.
 */
class RxProgressDialog(private val builder: Builder, val context: Context) {

    init {

    }

    private fun makeDialog(): ProgressDialog {
        val progressDialog: ProgressDialog = ProgressDialog(context)
        progressDialog.isIndeterminate = true
        if (builder.messageRes != null) {
            progressDialog.setMessage(context.getString(builder.messageRes!!))
        } else if (builder.messageText != null) {
            progressDialog.setMessage(builder.messageText!!)
        }

        return progressDialog
    }

    fun show(): Single<Any> {
        return Single.using({
            val dialog = makeDialog()
            dialog.show()
            dialog
        }, {
            builder.observable.toSingle()
        }, { dialog -> dialog.dismiss() })
    }

    class Builder(var observable: Observable<out Any>) {
        var messageText: String? = null
        var messageRes: Int? = null

        fun setMessage(message: String): Builder {
            this.messageRes = null
            this.messageText = message

            return this
        }

        fun setMessage(res: Int): Builder {
            this.messageText = null
            this.messageRes = res

            return this
        }

        fun setObservable(observable: Observable<out Any>): Builder {
            this.observable = observable
            return this
        }

        fun create(context: Context): RxProgressDialog {
            return RxProgressDialog(this, context)
        }
    }
}