package kr.ac.snu.hcil.omnitrack.core.system

import android.content.SharedPreferences
import com.github.salomonbrys.kotson.keys
import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.core.di.global.UserInfo
import kr.ac.snu.hcil.omnitrack.core.flags.LockFlagLevel
import kr.ac.snu.hcil.omnitrack.core.flags.LockedPropertiesHelper
import kr.ac.snu.hcil.omnitrack.core.serialization.getBooleanCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OTAppFlagManager @Inject constructor(
        @UserInfo private val sharedPreferences: SharedPreferences
) {
    companion object {
        const val PREF_APP_FLAG_KEYSET = "app_flag_keyset"

        const val PREF_PREFIX = "lockedProp:"

        fun toPreferenceKey(flagName: String): String {
            return "$PREF_PREFIX$flagName"
        }
    }

    fun updateAppFlags(flags: JsonObject?) {
        if (flags != null) {
            sharedPreferences.edit()
                    .putStringSet(PREF_APP_FLAG_KEYSET, flags.keys().map { toPreferenceKey(it) }.toSet())
                    .apply {
                        for (key in flags.keys()) {
                            val prefKey = toPreferenceKey(key)
                            val flagValue = flags.getBooleanCompat(key)
                            if (flagValue != null) {
                                this.putBoolean(prefKey, flagValue)
                            }
                        }
                    }
                    .apply()
        }
    }

    fun flag(flag: String): Boolean {
        val prefKey = toPreferenceKey(flag)
        if (sharedPreferences.contains(prefKey)) {
            return sharedPreferences.getBoolean(prefKey, true)
        } else {
            return if (BuildConfig.DEFAULT_EXPERIMENT_ID != null) {
                LockedPropertiesHelper.getDefaultValue(LockFlagLevel.App, flag)
            } else true
        }
    }
}