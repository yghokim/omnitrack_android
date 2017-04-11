package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.widget.Button
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.RemoteConfigManager
import rx.Subscription
import rx.internal.util.SubscriptionList

/**
 * Created by Young-Ho Kim on 2017-04-10.
 */
class VersionCheckDialogFragment : DialogFragment() {

    companion object {

        const val EXTRA_LATEST_VERSION = "latest_version"
        const val EXTRA_SHOW_DIALOG_ONCE_PER_UPDATE = "show_dialog_per_update"
        const val PREF_LAST_NOTIFIED_VERSION = "last_notified_version"

        fun makeInstance(): VersionCheckDialogFragment {
            return VersionCheckDialogFragment()
        }

        fun makeInstance(alreadyCheckedLatestVersion: String?, showDialogOncePerUpdate: Boolean = false): VersionCheckDialogFragment {
            val instance = VersionCheckDialogFragment()
            val bundle = Bundle()
            bundle.putString(EXTRA_LATEST_VERSION, alreadyCheckedLatestVersion)
            bundle.putBoolean(EXTRA_SHOW_DIALOG_ONCE_PER_UPDATE, showDialogOncePerUpdate)
            instance.arguments = bundle
            return instance
        }
    }

    private lateinit var currentVersionTextView: TextView
    private lateinit var latestVersionTextView: TextView
    private lateinit var storeButton: Button
    private lateinit var storeCaption: View

    private var latestVersion: String? = null

    private val dialogSubscriptions = SubscriptionList()

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

        val inflater = activity.layoutInflater
        val view = inflater.inflate(R.layout.layout_version_compare, null)
        setupViews(view)

        latestVersion = arguments?.getString(EXTRA_LATEST_VERSION)
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

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)

        println("dialog dismissed")
        if (arguments?.getBoolean(EXTRA_SHOW_DIALOG_ONCE_PER_UPDATE, false) == true) {
            if (latestVersion != null) {
                println("store to preference : ${latestVersion}")
                OTApplication.app.systemSharedPreferences.edit()
                        .putString(PREF_LAST_NOTIFIED_VERSION, latestVersion)
                        .apply()
            }
        }
        dialogSubscriptions.clear()
    }

    private fun setupViews(view: View) {
        currentVersionTextView = view.findViewById(R.id.ui_current_version) as TextView
        latestVersionTextView = view.findViewById(R.id.ui_latest_version) as TextView
        storeButton = view.findViewById(R.id.ui_store_button) as Button

        currentVersionTextView.text = BuildConfig.VERSION_NAME
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

        if (RemoteConfigManager.isNewVersionGreater(BuildConfig.VERSION_NAME, latestVersion)) {
            storeButton.isEnabled = true
            storeButton.setText(R.string.msg_button_label_new_version_available)
            storeCaption.visibility = View.VISIBLE
        } else {
            storeButton.isEnabled = false
            storeButton.setText(R.string.msg_button_label_latest_version)
            storeCaption.visibility = View.GONE
        }
    }

    private fun startLoadingVersion(): Subscription {
        return RemoteConfigManager.getServerLatestVersionName().subscribe({
            versionName ->
            this.latestVersion = versionName
            compareVersions(versionName)
        }, {
            storeButton.isEnabled = false
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialogSubscriptions.clear()
    }
}