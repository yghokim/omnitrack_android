package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.content.Intent
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatImageButton
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import com.badoo.mobile.util.WeakHandler
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.backend.OTAuthManager
import kr.ac.snu.hcil.omnitrack.ui.pages.AboutActivity
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.applyTint

/**
 * Created by Young-Ho Kim on 2017-01-31.
 */
class SidebarWrapper(val view: View, val parentActivity: AppCompatActivity) : PopupMenu.OnMenuItemClickListener {

    private val photoView: CircleImageView = view.findViewById(R.id.ui_user_photo) as CircleImageView
    private val nameView: TextView = view.findViewById(R.id.ui_user_name) as TextView
    private val profileMenuButton: AppCompatImageButton = view.findViewById(R.id.ui_button_profile_menu) as AppCompatImageButton

    private val menuListGroup: ViewGroup = view.findViewById(R.id.ui_menu_group) as ViewGroup

    init {
        /*
        val signOutButton = view.findViewById(R.id.ui_button_sign_out)
        signOutButton.setOnClickListener {
            if (AWSMobileClient.defaultMobileClient().identityManager.isUserSignedIn) {
                AWSMobileClient.defaultMobileClient().identityManager.signOut()
            } else {
                val intent = Intent(parentActivity, SignInActivity::class.java)
                parentActivity.startActivity(intent)
                parentActivity.finish()
            }
        }*/

        val popupMenu = PopupMenu(parentActivity, profileMenuButton, Gravity.TOP or Gravity.END)
        popupMenu.inflate(R.menu.menu_sidebar_profile)
        popupMenu.setOnMenuItemClickListener(this)

        profileMenuButton.setOnClickListener {
            popupMenu.show()
        }

        val aboutButton = view.findViewById(R.id.ui_button_about)
        aboutButton.setOnClickListener {
            val intent = Intent(parentActivity, AboutActivity::class.java)
            parentActivity.startActivity(intent)
        }

        val iconColor = ContextCompat.getColor(parentActivity, R.color.buttonIconColorDark)
        for (i in (0..menuListGroup.childCount - 1)) {
            val child = menuListGroup.getChildAt(i)
            if (child is Button) {
                val compoundDrawables = child.compoundDrawablesRelative
                for (k in (0..3)) {
                    compoundDrawables[k]?.let {
                        compoundDrawables[k] = applyTint(it, iconColor)
                    }
                }
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_unlink_with_this_device -> {
                DialogHelper.makeYesNoDialogBuilder(parentActivity, "OmniTrack", parentActivity.getString(R.string.msg_profile_unlink_account_confirm), {
                    OTAuthManager.signOut()
                    OTApplication.app.unlinkUser()
                }).show()
                return true
            }
            else -> {
                return false
            }
        }
    }

    fun refresh(user: OTUser) {
        WeakHandler().post {
            Glide.with(parentActivity).load(user.photoUrl).into(photoView)
            nameView.text = user.name
        }
    }
}