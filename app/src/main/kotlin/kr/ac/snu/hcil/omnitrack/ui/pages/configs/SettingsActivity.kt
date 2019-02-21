package kr.ac.snu.hcil.omnitrack.ui.pages.configs

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.*
import com.afollestad.materialdialogs.MaterialDialog
import com.github.javiersantos.appupdater.AppUpdater
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import kotlinx.android.synthetic.main.common_toolbar_with_buttons.*
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.di.configured.Backend
import kr.ac.snu.hcil.omnitrack.core.system.OTExternalSettingsPrompter
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.services.OTDeviceStatusService
import kr.ac.snu.hcil.omnitrack.services.OTVersionCheckWorker
import kr.ac.snu.hcil.omnitrack.ui.components.common.preference.ColorPreference
import kr.ac.snu.hcil.omnitrack.ui.components.common.preference.ColorPreferenceDialogFragment
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.LocaleHelper
import kr.ac.snu.hcil.omnitrack.utils.TextHelper
import javax.inject.Inject
import javax.inject.Provider


/**
 * Created by younghokim on 2017. 5. 19..
 */
class SettingsActivity : AppCompatActivity() {

    companion object {
        const val PREF_REMINDER_NOTI_RINGTONE = "pref_reminder_noti_ringtone"
        const val PREF_REMINDER_LIGHT_COLOR = "pref_reminder_light_color"
        const val PREF_CATEGORY_GENERAL = "pref_key_category_general"

        const val FLAG_CONFIGURATION_CHANGED = "flag_configuration_changed"

        const val REQUEST_CODE = 3423

        const val REQUEST_CODE_RINGTONE = 3452
    }

    private var fragment: SettingsFragment? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContextWithLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_multibutton_single_fragment)
        ui_appbar_title.text = title
        ui_appbar_button_left.setOnClickListener { finish() }
        ui_appbar_button_right.visibility = View.GONE

        fragment = supportFragmentManager.findFragmentById(R.id.ui_content_replace) as? SettingsFragment
                ?: SettingsFragment().apply {
                    this@SettingsActivity.supportFragmentManager.beginTransaction()
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

    class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {

        @Inject
        protected lateinit var currentSignedInLevel: Provider<OTAuthManager.SignedInLevel>

        @Inject
        protected lateinit var authManager: Provider<OTAuthManager>

        @Inject
        protected lateinit var dbManager: Lazy<BackendDbManager>

        @field:[Inject Backend]
        protected lateinit var realmProvider: Factory<Realm>

        @Inject
        protected lateinit var versionCheckController: Lazy<OTVersionCheckWorker.Controller>

        @Inject
        protected lateinit var shortcutPanelManager: OTShortcutPanelManager

        private var turnOnIgnoreBatteryOptimizationForwardingDialog: MaterialDialog? = null

        @TargetApi(23)
        private lateinit var ignoreBatteryOptimizationPreference: SwitchPreference

        private lateinit var settingsPrompter: OTExternalSettingsPrompter


        private var languageOnCreation: String? = null

        private val creationSubscriptions = CompositeDisposable()

        var resultCode: Int = Activity.RESULT_CANCELED
            private set

        var resultData: Intent? = null
            private set

        init {
            retainInstance = true
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.global_preferences, rootKey)
        }


        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            settingsPrompter = OTExternalSettingsPrompter(this.requireActivity())

            if (Build.VERSION.SDK_INT >= 23) {
                this.ignoreBatteryOptimizationPreference = SwitchPreference(context).apply {
                    this.key = OTExternalSettingsPrompter.KEY_IGNORE_BATTERY_OPTIMIZATION
                    this.isIconSpaceReserved = false
                    this.setTitle(R.string.msg_pref_title_ignore_battery_optimization)
                    this.setSummary(R.string.msg_pref_summary_ignore_battery_optimization)
                    this.isChecked = settingsPrompter.isBatteryOptimizationWhiteListed()
                }

                this.ignoreBatteryOptimizationPreference.setOnPreferenceClickListener {
                    if (settingsPrompter.isBatteryOptimizationWhiteListed()) {
                        //off
                        if (turnOnIgnoreBatteryOptimizationForwardingDialog == null) {
                            turnOnIgnoreBatteryOptimizationForwardingDialog = DialogHelper
                                    .makeSimpleAlertBuilder(requireActivity(),
                                            TextHelper.fromHtml(
                                                    String.format(getString(R.string.msg_pref_dialog_content_ignore_battery_optimization_off), BuildConfig.APP_NAME)
                                            ),
                                            R.string.msg_open) {
                                        settingsPrompter.askUserBatterOptimizationWhitelist()
                                    }
                                    .cancelable(true)
                                    .canceledOnTouchOutside(true)
                                    .build()
                        }

                        turnOnIgnoreBatteryOptimizationForwardingDialog?.show()
                    } else settingsPrompter.askUserBatterOptimizationWhitelist()
                    return@setOnPreferenceClickListener true
                }

                val generalCategory = findPreference(PREF_CATEGORY_GENERAL) as PreferenceCategory
                generalCategory.addPreference(this.ignoreBatteryOptimizationPreference)
            }

            findPreference<Preference>(PREF_REMINDER_NOTI_RINGTONE)?.run {
                summary = getCurrentReminderNotificationRingtoneName()
                onPreferenceChangeListener = this@SettingsFragment
            }

            findPreference<ListPreference>(LocaleHelper.PREF_KEY_SELECTED_LANGUAGE)?.run {
                this.let { pref ->
                    summary = pref.entry
                }
                onPreferenceChangeListener = this@SettingsFragment
            }
        }


        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == REQUEST_CODE_RINGTONE) {
                if (data != null) {
                    val ringtone: Uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                    preferenceManager.sharedPreferences.edit {
                        putString(PREF_REMINDER_NOTI_RINGTONE, ringtone.toString())
                    }
                    findPreference<Preference>(PREF_REMINDER_NOTI_RINGTONE).summary = RingtoneManager.getRingtone(requireContext(), ringtone).getTitle(requireContext())

                }
            } else {
                val result = settingsPrompter.handleActivityResult(requestCode, resultCode, data)
                if (result?.first == OTExternalSettingsPrompter.KEY_IGNORE_BATTERY_OPTIMIZATION) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        this.ignoreBatteryOptimizationPreference.isChecked = settingsPrompter.isBatteryOptimizationWhiteListed()
                    }

                    if (settingsPrompter.isBatteryOptimizationWhiteListed()) {
                        requireActivity().startService(Intent(requireContext(), OTDeviceStatusService::class.java))
                    }
                }
            }
        }

        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)

            (requireActivity().application as OTAndroidApp).currentConfiguredContext.configuredAppComponent.inject(this)
            languageOnCreation = LocaleHelper.getLanguageCode(requireContext())
        }

        override fun onDestroy() {
            super.onDestroy()
            findPreference<Preference>(PREF_REMINDER_NOTI_RINGTONE).onPreferenceChangeListener = null
            findPreference<ListPreference>(LocaleHelper.PREF_KEY_SELECTED_LANGUAGE).onPreferenceChangeListener = null
            creationSubscriptions.clear()
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)

            if (Build.VERSION.SDK_INT >= 23) {
                this.ignoreBatteryOptimizationPreference.isChecked = settingsPrompter.isBatteryOptimizationWhiteListed()
            }
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

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            if (preference.key == PREF_REMINDER_NOTI_RINGTONE) {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, DEFAULT_NOTIFICATION_URI)

                val ringtoneUri = Uri.parse(preferenceManager.sharedPreferences.getString(PREF_REMINDER_NOTI_RINGTONE, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.toString()))
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri)

                startActivityForResult(intent, REQUEST_CODE_RINGTONE)

                return true
            } else return super.onPreferenceTreeClick(preference)
        }

        override fun onDisplayPreferenceDialog(preference: Preference?) {
            if (preference is ColorPreference) {
                val colorDialogFragment = ColorPreferenceDialogFragment.makeInstance(preference.key)
                colorDialogFragment.setTargetFragment(this, 0)
                colorDialogFragment.show(requireFragmentManager(), "ColorPreferenceDialog")

            } else super.onDisplayPreferenceDialog(preference)

        }

        override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
            println("preference changed - ${preference.key}, $newValue")
            if (preference.key == LocaleHelper.PREF_KEY_SELECTED_LANGUAGE && preference is ListPreference) {
                if (newValue is String) {
                    try {
                        preference.summary = preference.entries[preference.findIndexOfValue(newValue)]
                    } catch (ex: Exception) {
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

                            authManager.get().userId?.let { userId ->
                                val realm = realmProvider.get()
                                creationSubscriptions.add(
                                        dbManager.get().makeShortcutPanelRefreshObservable(userId, realm).toObservable()
                                                .firstElement().subscribe { realm.close() })
                            }

                        } else {
                            shortcutPanelManager.disposeShortcutPanel()
                        }
                    } else {
                        shortcutPanelManager.disposeShortcutPanel()
                    }
                }

                AppUpdater.PREF_CHECK_UPDATES -> {
                    if (sharedPreferences.getBoolean(key, false)) {
                        versionCheckController.get().checkVersionOneTime()
                    } else {
                        versionCheckController.get().cancelVersionCheckingWork()
                    }
                }

                PREF_REMINDER_NOTI_RINGTONE -> {
                    println("ringtone was changed to ${sharedPreferences.getString(PREF_REMINDER_NOTI_RINGTONE, "")}")
                    findPreference<Preference>(key).summary = getCurrentReminderNotificationRingtoneName()
                }

                LocaleHelper.PREF_USE_DEVICE_DEFAULT, LocaleHelper.PREF_KEY_SELECTED_LANGUAGE -> {
                    (requireActivity().application as OTAndroidApp).refreshConfiguration(requireActivity())
                    Toast.makeText(activity, R.string.msg_language_change_applied_after_exit_this_screen, Toast.LENGTH_LONG).show()
                    setResult(Activity.RESULT_OK, Intent().apply { putExtra(FLAG_CONFIGURATION_CHANGED, true) })
                    println("activity: $activity")
                }
            }
        }
    }
}