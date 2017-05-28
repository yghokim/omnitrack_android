package kr.ac.snu.hcil.omnitrack.ui.pages.settings

import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.backend.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.services.OTVersionCheckService
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.utils.LocaleHelper


/**
 * Created by younghokim on 2017. 5. 19..
 */
class SettingsActivity : MultiButtonActionBarActivity(R.layout.activity_multibutton_single_fragment) {

    companion object {
        const val PREF_REMINDER_NOTI_RINGTONE = "pref_reminder_noti_ringtone"
        const val PREF_REMINDER_LIGHT_COLOR = "pref_reminder_light_color"
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

        init {
            retainInstance = true
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.global_preferences)

            findPreference(PREF_REMINDER_NOTI_RINGTONE)?.run {
                summary = getCurrentReminderNotificationRingtoneName()
                onPreferenceChangeListener = this@SettingsFragment
            }

            findPreference(LocaleHelper.PREF_KEY_SELECTED_LANGUAGE)?.run {
                (this as? ListPreference)?.let {
                    pref ->
                    summary = pref.entries[pref.findIndexOfValue(LocaleHelper.getNearestLanguageToDevice(activity))]
                }
                onPreferenceChangeListener = this@SettingsFragment
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            findPreference(PREF_REMINDER_NOTI_RINGTONE).setOnPreferenceChangeListener(null)
            findPreference(LocaleHelper.PREF_KEY_SELECTED_LANGUAGE).setOnPreferenceChangeListener(null)
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
            } else if (preference.key == LocaleHelper.PREF_KEY_SELECTED_LANGUAGE && preference is ListPreference) {
                if (newValue is String) {
                    try {
                        preference.summary = preference.entries[preference.findIndexOfValue(newValue)]
                    } catch(ex: Exception) {
                        preference.summary = newValue
                    }

                    OTApplication.app.refreshConfiguration(activity)
                    activity.recreate()
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