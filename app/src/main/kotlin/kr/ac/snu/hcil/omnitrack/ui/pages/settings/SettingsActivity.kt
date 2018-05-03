package kr.ac.snu.hcil.omnitrack.ui.pages.settings

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.widget.Toast
import com.github.javiersantos.appupdater.AppUpdater
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.di.configured.Backend
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.services.OTVersionCheckService
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.utils.LocaleHelper
import org.jetbrains.anko.act
import javax.inject.Inject
import javax.inject.Provider


/**
 * Created by younghokim on 2017. 5. 19..
 */
class SettingsActivity : MultiButtonActionBarActivity(R.layout.activity_multibutton_single_fragment) {

    companion object {
        const val PREF_REMINDER_NOTI_RINGTONE = "pref_reminder_noti_ringtone"
        const val PREF_REMINDER_LIGHT_COLOR = "pref_reminder_light_color"

        const val FLAG_CONFIGURATION_CHANGED = "flag_configuration_changed"

        const val REQUEST_CODE = 3423
    }

    private var fragment: SettingsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setActionBarButtonMode(Mode.Back)

        fragment = fragmentManager.findFragmentById(R.id.ui_content_replace) as? SettingsFragment ?: SettingsFragment().apply {
            this@SettingsActivity.fragmentManager.beginTransaction()
                    .replace(R.id.ui_content_replace, this)
                    .commit()
        }

    }

    override fun finish() {
        setResult(fragment?.resultCode ?: Activity.RESULT_CANCELED, fragment?.resultData)
        super.finish()
    }

    override fun onBackPressed() {
        setResult(fragment?.resultCode ?: Activity.RESULT_CANCELED, fragment?.resultData)
        super.onBackPressed()
    }

    override fun onToolbarLeftButtonClicked() {
        finish()
    }

    override fun onToolbarRightButtonClicked() {

    }

    class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {

        @Inject
        protected lateinit var currentSignedInLevel: Provider<OTAuthManager.SignedInLevel>

        @Inject
        protected lateinit var dbManager: Lazy<BackendDbManager>

        @field:[Inject Backend]
        protected lateinit var realmProvider: Factory<Realm>

        @Inject
        protected lateinit var versionCheckServiceController: Lazy<OTVersionCheckService.Controller>

        @Inject
        protected lateinit var shortcutPanelManager: OTShortcutPanelManager


        private var languageOnCreation: String? = null

        private val creationSubscriptions = CompositeDisposable()

        var resultCode: Int = Activity.RESULT_CANCELED
            private set

        var resultData: Intent? = null
            private set

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
                    summary = pref.entry
                }
                onPreferenceChangeListener = this@SettingsFragment
            }
        }

        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)

            (act.application as OTApp).currentConfiguredContext.configuredAppComponent.inject(this)
            languageOnCreation = LocaleHelper.getLanguageCode(activity)
        }

        override fun onDestroy() {
            super.onDestroy()
            findPreference(PREF_REMINDER_NOTI_RINGTONE).onPreferenceChangeListener = null
            findPreference(LocaleHelper.PREF_KEY_SELECTED_LANGUAGE).onPreferenceChangeListener = null
            creationSubscriptions.clear()
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

        fun setResult(resultCode: Int, data: Intent?) {
            this.resultCode = resultCode
            this.resultData = data
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
                        if (currentSignedInLevel.get() > OTAuthManager.SignedInLevel.NONE) {
                            val activity = activity
                            if (activity is OTActivity) {
                                val realm = realmProvider.get()

                                creationSubscriptions.add(
                                        activity.signedInUserObservable.concatMap { userId ->
                                            dbManager.get().makeShortcutPanelRefreshObservable(userId, realm).toObservable()
                                        }
                                                .firstElement().subscribe { realm.close() })
                            } else {
                                shortcutPanelManager.disposeShortcutPanel()
                            }
                        } else {
                            shortcutPanelManager.disposeShortcutPanel()
                        }
                    } else {
                        shortcutPanelManager.disposeShortcutPanel()
                    }
                }

                AppUpdater.PREF_CHECK_UPDATES-> {
                    if(sharedPreferences.getBoolean(key, false))
                    {
                        versionCheckServiceController.get().turnOnService()
                    }
                    else{
                        versionCheckServiceController.get().turnOffService()
                    }
                }

                PREF_REMINDER_NOTI_RINGTONE -> {
                    println("ringtone was changed to ${sharedPreferences.getString(PREF_REMINDER_NOTI_RINGTONE, "")}")
                    findPreference(key).summary = getCurrentReminderNotificationRingtoneName()
                }

                LocaleHelper.PREF_USE_DEVICE_DEFAULT, LocaleHelper.PREF_KEY_SELECTED_LANGUAGE -> {
                    OTApp.instance.refreshConfiguration(activity)
                    Toast.makeText(activity, R.string.msg_language_change_applied_after_exit_this_screen, Toast.LENGTH_LONG).show()
                    setResult(Activity.RESULT_OK, Intent().apply { putExtra(FLAG_CONFIGURATION_CHANGED, true) })
                    println("activity: ${activity}")
                }
            }
        }
    }
}