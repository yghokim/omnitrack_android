package kr.ac.snu.hcil.omnitrack.ui.pages.export

import android.app.AlertDialog
import android.app.Application
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.transition.TransitionManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.dialog_temporary_package_upload.view.*
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
import javax.inject.Inject

class UploadTemporaryPackageDialogFragment : DialogFragment(), View.OnClickListener {

    companion object {
        fun makeInstance(jsonString: String): UploadTemporaryPackageDialogFragment {
            return UploadTemporaryPackageDialogFragment().apply {
                arguments = bundleOf(PackageHandlingBottomSheetFragment.KEY_JSON_CONTENT_STRING to jsonString)
            }
        }
    }

    private lateinit var dialogView: View

    private lateinit var viewModel: ViewModel

    private val subscriptions: CompositeDisposable by lazy {
        CompositeDisposable()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ViewModel::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val args = arguments
        if (args == null || !args.containsKey(PackageHandlingBottomSheetFragment.KEY_JSON_CONTENT_STRING)) {
            throw IllegalArgumentException("No content string argument passed before showing the dialog.")
        }

        viewModel.tryShare(args.getString(PackageHandlingBottomSheetFragment.KEY_JSON_CONTENT_STRING))

        this.dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_temporary_package_upload, null, false)

        this.dialogView.ui_button.text = "Cancel"
        this.dialogView.ui_button.setOnClickListener(this)

        val dialog = AlertDialog.Builder(requireActivity())
                .setView(this.dialogView)
                .create()

        subscriptions.add(
                viewModel.receivedCodeSubject.subscribe {
                    TransitionManager.beginDelayedTransition(this.dialogView as ViewGroup)
                    this.dialogView.ui_group_loading.visibility = View.GONE
                    this.dialogView.ui_group_complete.visibility = View.VISIBLE
                    this.dialogView.ui_button.text = "Complete"
                    this.dialogView.ui_access_key_indicator.text = it
                }
        )

        return dialog
    }

    override fun onDetach() {
        super.onDetach()
        subscriptions.clear()
    }

    override fun onClick(v: View) {
        if (v === this.dialogView.ui_button) {
            viewModel.cancel()
            dismiss()
        }
    }

    class ViewModel(application: Application) : AndroidViewModel(application) {

        private lateinit var currentJsonString: String

        private val receivedCodeBehaviorSubject = BehaviorSubject.create<String>()
        val receivedCodeSubject: Subject<String> get() = receivedCodeBehaviorSubject

        private val subscriptions: CompositeDisposable by lazy {
            CompositeDisposable()
        }

        @Inject
        protected lateinit var serverApi: ISynchronizationServerSideAPI

        init {
            (application as OTAndroidApp).applicationComponent.inject(this)
        }

        override fun onCleared() {
            super.onCleared()
            subscriptions.clear()
        }

        fun tryShare(jsonString: String) {
            if (!this::currentJsonString.isInitialized || (this::currentJsonString.isInitialized && currentJsonString != jsonString)) {
                currentJsonString = jsonString
                subscriptions.add(
                        serverApi.uploadTemporaryTrackingPackage(jsonString).subscribe { code ->
                            receivedCodeBehaviorSubject.onNext(code)
                        }
                )
            }
        }

        fun cancel() {
            subscriptions.clear()
        }
    }

}