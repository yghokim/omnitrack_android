package kr.ac.snu.hcil.omnitrack.ui.pages.settings

import android.os.Bundle
import android.preference.PreferenceFragment
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity

/**
 * Created by younghokim on 2017. 5. 19..
 */
class SettingsActivity : MultiButtonActionBarActivity(R.layout.activity_multibutton_single_fragment) {
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

    class SettingsFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.global_preferences)
        }
    }
}