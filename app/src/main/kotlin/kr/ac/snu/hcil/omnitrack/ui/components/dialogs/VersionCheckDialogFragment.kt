package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.widget.Button
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.RemoteConfigManager
import rx.Subscription
import rx.internal.util.SubscriptionList

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
            bundle.putString("latest_version", alreadyCheckedLatestVersion)
            instance.arguments = bundle
            return instance
        }
    }

    private lateinit var currentVersionTextView: TextView
    private lateinit var latestVersionTextView: TextView
    private lateinit var storeButton: Button

    private val startSubscriptions = SubscriptionList()

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

        var subscription: Subscription? = null

        val latestVersion = arguments?.getString("latest_version")
        if (latestVersion != null) {
            compareVersions(latestVersion)
        } else {
            subscription = startLoadingVersion()
        }

        return AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(true)
                .setNegativeButton(getString(R.string.msg_close), null)
                .setOnDismissListener {
                    subscription?.unsubscribe()
                }
                .create()
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
    }

    private fun compareVersions(latestVersion: String) {
        latestVersionTextView.text = latestVersion

        if (BuildConfig.DEBUG || RemoteConfigManager.isNewVersionGreater(BuildConfig.VERSION_NAME, latestVersion)) {
            storeButton.isEnabled = true
            storeButton.setText(R.string.msg_button_label_new_version_available)
        } else {
            storeButton.isEnabled = false
            storeButton.setText(R.string.msg_button_label_latest_version)
        }
    }

    private fun startLoadingVersion(): Subscription {
        return RemoteConfigManager.getServerLatestVersionName().subscribe({
            versionName ->
            compareVersions(versionName)
        }, {
            storeButton.isEnabled = false
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        startSubscriptions.clear()
    }
}