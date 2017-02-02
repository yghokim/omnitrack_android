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
        println("OMNITRACK signin process finished in splashactivity.")
        creationSubscriptions.add(
                signedInUserObservable.subscribe {
                    user ->
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.putExtra(OTApplication.INTENT_EXTRA_IGNORE_SIGN_IN_CHECK, true)
                    startActivity(intent)
                    finish()
                }
        )
    }
}