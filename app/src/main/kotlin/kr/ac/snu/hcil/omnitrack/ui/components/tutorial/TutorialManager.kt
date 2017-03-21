package kr.ac.snu.hcil.omnitrack.ui.components.tutorial

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.view.MotionEvent
import android.view.View
import kr.ac.snu.hcil.omnitrack.OTApplication
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

/**
 * Created by younghokim on 2017. 3. 21..
 */
object TutorialManager {

    val DEBUG_ALWAYS_SHOW_TUTORIAL = false

    const val PREFERENCE_NAME = "pref_tutorial_flags"

    private fun makeFlagKey(tag: String): String {
        return "pref_tutorial_shown_flag_${tag}"
    }

    private val preferences: SharedPreferences by lazy {
        OTApplication.app.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    fun hasShownTutorials(tag: String): Boolean {
        return preferences.getBoolean(makeFlagKey(tag), false)
    }

    fun setTutorialFlag(tag: String, flag: Boolean) {
        preferences.edit().putBoolean(makeFlagKey(tag), flag).apply()
    }

    fun checkAndShowTargetPrompt(tag: String, closeFlagAfterClose: Boolean, activity: Activity, target: View, primaryText: String?, secondaryText: String?, backgroundColor: Int): Boolean {
        if (DEBUG_ALWAYS_SHOW_TUTORIAL || !hasShownTutorials(tag)) {
            val builder = MaterialTapTargetPrompt.Builder(activity)
                    .setTarget(target)
                    .setBackgroundColour(backgroundColor)
                    .setOnHidePromptListener(object : MaterialTapTargetPrompt.OnHidePromptListener {
                        override fun onHidePromptComplete() {
                            if (closeFlagAfterClose) {
                                setTutorialFlag(tag, true)
                            }
                        }

                        override fun onHidePrompt(event: MotionEvent?, tappedTarget: Boolean) {

                        }

                    })
            if (primaryText != null) {
                builder.setPrimaryText(primaryText)
            }

            if (secondaryText != null) {
                builder.setSecondaryText(secondaryText)
            }

            builder.show()

            return true
        } else return false
    }

    fun checkAndShowTargetPrompt(tag: String, closeFlagAfterClose: Boolean, activity: Activity, target: View, primaryTextRes: Int, secondaryTextRes: Int, backgroundColor: Int): Boolean {
        return checkAndShowTargetPrompt(tag, closeFlagAfterClose, activity, target, activity.resources.getString(primaryTextRes), activity.resources.getString(secondaryTextRes), backgroundColor)
    }
}