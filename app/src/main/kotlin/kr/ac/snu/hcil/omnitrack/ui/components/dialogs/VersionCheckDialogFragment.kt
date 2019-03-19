package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.github.javiersantos.appupdater.AppUpdaterUtils
import com.github.javiersantos.appupdater.enums.AppUpdaterError
import com.github.javiersantos.appupdater.objects.Update
import io.reactivex.disposables.CompositeDisposable
import kr.ac.snu.hcil.android.common.versionName
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.system.OTAppVersionCheckManager

/**
 * Created by Young-Ho Kim on 2017-04-10.
 */
class VersionCheckDialogFragment : DialogFragment() {

    companion object {

        fun makeInstance(): VersionCheckDialogFragment {
            return VersionCheckDialogFragment()
        }
    }

    private lateinit var currentVersionTextView: TextView
    private lateinit var latestVersionTextView: TextView
    private lateinit var storeButton: Button

    private var latestVersion: String? = null

    private val dialogSubscriptions = CompositeDisposable()

    private lateinit var appUpdaterUtils: AppUpdaterUtils
    private var latestUpdateInfo: Update? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appUpdaterUtils = OTAppVersionCheckManager.makeAppUpdaterUtils(requireContext())
                .withListener(object : AppUpdaterUtils.UpdateListener {
                    override fun onSuccess(update: Update, isUpdateAvailable: Boolean) {

                        latestVersion = update.latestVersion
                        latestVersionTextView.text = latestVersion
                        latestUpdateInfo = update

                        if (isUpdateAvailable) {
                            storeButton.isEnabled = true
                            storeButton.setText(R.string.msg_button_label_new_version_available)
                        } else {
                            storeButton.isEnabled = false
                            storeButton.setText(R.string.msg_button_label_latest_version)
                        }
                    }

                    override fun onFailed(error: AppUpdaterError) {
                        println(error.name)
                        storeButton.isEnabled = false
                    }

                })
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.layout_version_compare, null)
        setupViews(view)

        appUpdaterUtils.start()

        return AlertDialog.Builder(requireActivity())
                .setView(view)
                .setCancelable(true)
                .setNegativeButton(getString(R.string.msg_close), null)
                .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        appUpdaterUtils.stop()
    }

    private fun setupViews(view: View) {
        currentVersionTextView = view.findViewById(R.id.ui_current_version)
        latestVersionTextView = view.findViewById(R.id.ui_latest_version)
        storeButton = view.findViewById(R.id.ui_store_button)

        currentVersionTextView.text = view.context.versionName()
        storeButton.isEnabled = false
        storeButton.setOnClickListener {

            if (latestUpdateInfo != null) {
                appUpdaterUtils.goToUpdate(latestUpdateInfo!!)
            }

            this.dialog?.dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialogSubscriptions.clear()
    }
}