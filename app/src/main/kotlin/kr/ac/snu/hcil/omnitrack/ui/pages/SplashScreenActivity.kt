package kr.ac.snu.hcil.omnitrack.ui.pages

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper


/**
 * Created by Young-Ho on 8/1/2016.
 * https://www.bignerdranch.com/blog/splash-screens-the-right-way/
 */
class SplashScreenActivity : OTActivity(checkRefreshingCredential = true, checkUpdateAvailable = false) {

    override val isSessionLoggingEnabled: Boolean = false

    private val LOG_TAG = SplashScreenActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pref = getSharedPreferences("app_info", Context.MODE_PRIVATE)

        if (pref.getBoolean("first_launch in this system", false)) {
            //first run
        }
    }

    override fun processAuthorization() {

        //check google play service availability

        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val googlePlayServiceStatus = googleApiAvailability.isGooglePlayServicesAvailable(this.applicationContext)

        if (googlePlayServiceStatus != ConnectionResult.SUCCESS) {
            //google play service is not available for now.
            if (googleApiAvailability.isUserResolvableError(googlePlayServiceStatus)) {
                googleApiAvailability.makeGooglePlayServicesAvailable(this).addOnCompleteListener {
                    result ->
                    if (result.isSuccessful) {
                        super.processAuthorization()
                    } else {
                        finish()
                    }
                }
            } else {
                //unsupported device.
                DialogHelper.makeSimpleAlertBuilder(this, "This device may not to support Google Play services, which is required to run the OmniTrack instance. If you are in the region such as China, where the Google Play service is unavailable, we're sorry to confirm that you can't use OmniTrack :(")
                        .dismissListener {
                            finish()
                        }.show()
            }
        } else {
            super.processAuthorization()
        }
    }

    override fun onSignInProcessCompletelyFinished() {
        super.onSignInProcessCompletelyFinished()
        println("OMNITRACK signin process finished in splashactivity.")
        creationSubscriptions.add(
                signedInUserObservable.subscribe {
                    user ->
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.putExtra(OTApp.INTENT_EXTRA_IGNORE_SIGN_IN_CHECK, true)
                    startActivity(intent)
                    finish()
                }
        )
    }
}