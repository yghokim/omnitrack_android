package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import com.amazonaws.activities.SignInActivity
import com.amazonaws.mobile.AWSMobileClient
import com.amazonaws.mobile.user.IdentityProvider
import com.amazonaws.mobile.util.ThreadUtils
import de.hdodenhof.circleimageview.CircleImageView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.pages.AboutActivity

/**
 * Created by Young-Ho Kim on 2017-01-31.
 */
class SidebarWrapper(val view: View, val parentActivity: AppCompatActivity) {
    private val photoView: CircleImageView = view.findViewById(R.id.ui_user_photo) as CircleImageView
    private val nameView: TextView = view.findViewById(R.id.ui_user_name) as TextView

    init {
        val signOutButton = view.findViewById(R.id.ui_button_sign_out)
        signOutButton.setOnClickListener {
            if (AWSMobileClient.defaultMobileClient().identityManager.isUserSignedIn) {
                AWSMobileClient.defaultMobileClient().identityManager.signOut()
            } else {
                val intent = Intent(parentActivity, SignInActivity::class.java)
                parentActivity.startActivity(intent)
                parentActivity.finish()
            }
        }

        val aboutButton = view.findViewById(R.id.ui_button_about)
        aboutButton.setOnClickListener {
            val intent = Intent(parentActivity, AboutActivity::class.java)
            parentActivity.startActivity(intent)
        }
    }

    fun refresh(provider: IdentityProvider) {
        val identityManager = AWSMobileClient.defaultMobileClient().identityManager
        //TODO cache image and user name in OTUser
        identityManager.loadUserInfoAndImage(provider) {
            ThreadUtils.runOnUiThread {
                photoView.setImageBitmap(identityManager.userImage)
                nameView.text = identityManager.userName
            }
        }
    }

    fun dispose() {

    }
}