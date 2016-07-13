package kr.ac.snu.hcil.omnitrack.activities

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.core.OTUser

/**
 * Created by Young-Ho Kim on 2016-07-13.
 */
open class UserSyncedActivity : AppCompatActivity() {

    lateinit protected var user: OTUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        user = (application as OmniTrackApplication).currentUser
    }

    override fun onPause() {
        super.onPause()
        (application as OmniTrackApplication).syncUserToDb()
    }
}