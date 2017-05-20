package kr.ac.snu.hcil.omnitrack.ui.pages.settings

import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.backend.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.services.OTVersionCheckService
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity


/**
 * Created by younghokim on 2017. 5. 19..
 */
class SettingsActivity : MultiButtonActionBarActivity(R.layout.activity_multibutton_single_fragment) {

    companion object {
        const val PREF_REMINDER_NOTI_RINGTONE = "pref_reminder_noti_ringtone"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setActionBarButtonMode(Mode.Back)

        fragmentManager.beginTransaction()
                .replace(R.id.ui_content_replace, SettingsFragment())
                .commit()

    }

    override fun onToolbarLeftButtonClicked() {
        finish()
    }

    override fun onToolbarRightButtonClicked() {

    }

    class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.global_preferences)

            findPreference(PREF_REMINDER_NOTI_RINGTONE)?.summary = getCurrentReminderNotificationRingtoneName()
            findPreference(PREF_REMINDER_NOTI_RINGTONE).setOnPreferenceChangeListener(this)
        }

        override fun onDestroy() {
            super.onDestroy()
            findPreference(PREF_REMINDER_NOTI_RINGTONE).setOnPreferenceChangeListener(null)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        fun getCurrentReminderNotificationRingtoneName(): String {
            val ringtoneUri = Uri.parse(preferenceManager.sharedPreferences.getString(PREF_REMINDER_NOTI_RINGTONE, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.toString()))
            val ringtone = RingtoneManager.getRingtone(activity, ringtoneUri)
            return ringtone.getTitle(activity)
        }

        override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
            println("preference changed - ${preference.key}, ${newValue}")
            if (preference.key == PREF_REMINDER_NOTI_RINGTONE) {
                if (newValue is String) {
                    val ringtone = RingtoneManager.getRingtone(activity, Uri.parse(newValue))
                    preference.summary = ringtone.getTitle(activity)
                    return true
                }
            }
            return false
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            when (key) {
                "pref_show_shortcut_panel" -> {
                    if (sharedPreferences.getBoolean(key, true)) {
                        //show if logged in
                        if (OTAuthManager.currentSignedInLevel > OTAuthManager.SignedInLevel.NONE) {
                            val activity = activity
                            if (activity is OTActivity) {
                                activity.signedInUserObservable.subscribe({
                                    user ->
                                    OTShortcutPanelManager.refreshNotificationShortcutViews(user, activity)
                                }, {
                                    OTShortcutPanelManager.disposeShortcutPanel()
                                })
                            }
                        } else {
                            OTShortcutPanelManager.disposeShortcutPanel()
                        }
                    } else {
                        OTShortcutPanelManager.disposeShortcutPanel()
                    }
                }

                "pref_check_updates" -> {
                    OTVersionCheckService.setupServiceAlarm(activity)
                }

                PREF_REMINDER_NOTI_RINGTONE -> {
                    println("ringtone was changed to ${sharedPreferences.getString(PREF_REMINDER_NOTI_RINGTONE, "")}")
                    findPreference(key).summary = getCurrentReminderNotificationRingtoneName()
                }
            }
        }
    }
}