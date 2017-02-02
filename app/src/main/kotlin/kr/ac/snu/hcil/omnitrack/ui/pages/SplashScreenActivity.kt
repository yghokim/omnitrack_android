package kr.ac.snu.hcil.omnitrack.ui.pages

import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity


/**
 * Created by Young-Ho on 8/1/2016.
 * https://www.bignerdranch.com/blog/splash-screens-the-right-way/
 */
class SplashScreenActivity : OTActivity(checkRefreshingCredential = true) {

    private val LOG_TAG = SplashScreenActivity::class.java.simpleName

    override fun onSignInProcessCompletelyFinished() {
        super.onSignInProcessCompletelyFinished()
        creationSubscriptions.add(
                OTApplication.app.currentUserObservable.subscribe {
                    user ->
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                }
        )
    }
}