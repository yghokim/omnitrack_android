package kr.ac.snu.hcil.omnitrack.ui.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity

/**
 * Created by Young-Ho on 8/1/2016.
 * https://www.bignerdranch.com/blog/splash-screens-the-right-way/
 */
class SplashScreenActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}