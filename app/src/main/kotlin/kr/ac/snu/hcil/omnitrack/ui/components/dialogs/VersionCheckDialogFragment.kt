package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import kr.ac.snu.hcil.android.common.versionName
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2017-04-10.
 */
class VersionCheckDialogFragment : DialogFragment() {

    companion object {

        fun makeInstance(): VersionCheckDialogFragment {
            return VersionCheckDialogFragment()
        }

        fun makeInstance(alreadyCheckedLatestVersion: String?): VersionCheckDialogFragment {
            val instance = VersionCheckDialogFragment()
            val bundle = Bundle()
            bundle.putString(OTApp.INTENT_EXTRA_LATEST_VERSION_NAME, alreadyCheckedLatestVersion)
            instance.arguments = bundle
            return instance
        }
    }

    private lateinit var currentVersionTextView: TextView
    private lateinit var latestVersionTextView: TextView
    private lateinit var storeButton: Button
    private lateinit var storeCaption: View

    private var latestVersion: String? = null

    private val dialogSubscriptions = CompositeDisposable()

    /*
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.layout_version_compare, container, false)

        setupViews(view)

        startSubscriptions.add(
                startLoadingVersion()
        )

        return view
    }*/

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val inflater = activity!!.layoutInflater
        val view = inflater.inflate(R.layout.layout_version_compare, null)
        setupViews(view)

        latestVersion = arguments?.getString(OTApp.INTENT_EXTRA_LATEST_VERSION_NAME)
        if (latestVersion != null) {
            compareVersions(latestVersion!!)
        } else {
            dialogSubscriptions.add(startLoadingVersion())
        }

        return AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(true)
                .setNegativeButton(getString(R.string.msg_close), null)
                .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        dialogSubscriptions.clear()
    }

    private fun setupViews(view: View) {
        currentVersionTextView = view.findViewById(R.id.ui_current_version)
        latestVersionTextView = view.findViewById(R.id.ui_latest_version)
        storeButton = view.findViewById(R.id.ui_store_button)

        currentVersionTextView.text = view.context.versionName()
        storeButton.isEnabled = false
        storeButton.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=kr.ac.snu.hcil.omnitrack")))
            } catch(ex: ActivityNotFoundException) {
                ex.printStackTrace()
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=kr.ac.snu.hcil.omnitrack")))
            }

            this.dialog?.dismiss()
        }

        storeCaption = view.findViewById(R.id.caption)
    }

    private fun compareVersions(latestVersion: String) {
        latestVersionTextView.text = latestVersion

        /* TODO replace logic with AppUpdater
        if (RemoteConfigManager.isNewVersionGreater(BuildConfig.VERSION_NAME, latestVersion)) {
            storeButton.isEnabled = true
            storeButton.setText(R.string.msg_button_label_new_version_available)
            storeCaption.visibility = View.VISIBLE
        } else {
            storeButton.isEnabled = false
            storeButton.setText(R.string.msg_button_label_latest_version)
            storeCaption.visibility = View.GONE
        }*/
    }

    private fun startLoadingVersion(): Disposable {
        /* TODO replace logic with AppUpdater
        return RemoteConfigManager.getServerLatestVersionName().subscribe({
            versionName ->
            this.latestVersion = versionName
            compareVersions(versionName)
        }, {
            storeButton.isEnabled = false
        })*/
        return Disposables.empty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialogSubscriptions.clear()
    }
}