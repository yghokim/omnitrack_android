
package kr.ac.snu.hcil.omnitrack.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.ui.components.ShortTextPropertyView
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
class TrackerSettingsActivity : OkCancelActivity() {

    private lateinit var user : OTUser
    private lateinit var tracker : OTTracker

    private lateinit var namePropertyView : ShortTextPropertyView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracker_settings)
        val toolbar = findViewById(R.id.toolbar) as Toolbar?
        setSupportActionBar(toolbar)

        user = (application as OmniTrackApplication).currentUser

        namePropertyView = findViewById(R.id.nameProperty) as ShortTextPropertyView

    }

    override fun onCanceled() {
        finish()
    }

    override fun onOk() {
        DialogHelper.makeYesNoDialogBuilder(this, tracker.name, resources.getString(R.string.msg_confirm_apply_settings), {-> applySettings(); finish() }).show()
    }

    private fun applySettings(){
        tracker.name = namePropertyView.value
        OmniTrackApplication.app.syncUserToDb()
    }

    override fun onStart() {
        super.onStart()
        tracker = user.trackers.filter{ it.objectId == intent.getStringExtra("trackerId") }.first()
        namePropertyView.value = tracker.name
    }
}