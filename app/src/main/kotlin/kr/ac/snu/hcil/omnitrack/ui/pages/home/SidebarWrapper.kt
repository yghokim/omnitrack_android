package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatImageButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import com.bumptech.glide.Glide
import dagger.Lazy
import de.hdodenhof.circleimageview.CircleImageView
import io.reactivex.disposables.CompositeDisposable
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.viewholders.RecyclerViewMenuAdapter
import kr.ac.snu.hcil.omnitrack.ui.pages.AboutActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.settings.SettingsActivity
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import javax.inject.Inject

/**
 * Created by Young-Ho Kim on 2017-01-31.
 */
class SidebarWrapper(val view: View, val parentActivity: AppCompatActivity) : PopupMenu.OnMenuItemClickListener {

    @Inject
    lateinit var authManager: OTAuthManager

    @Inject
    lateinit var syncManager: Lazy<OTSyncManager>

    private val photoView: CircleImageView = view.findViewById(R.id.ui_user_photo)
    private val nameView: TextView = view.findViewById(R.id.ui_user_name)
    private val profileMenuButton: AppCompatImageButton = view.findViewById(R.id.ui_button_profile_menu)

    private val menuList: RecyclerView = view.findViewById(R.id.ui_menu_list)

    private val subscriptions = CompositeDisposable()

    init {

        (parentActivity.application as OTApp).applicationComponent.inject(this)
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

        menuList.layoutManager = LinearLayoutManager(parentActivity, LinearLayoutManager.VERTICAL, false)
        menuList.adapter = SidebarMenuAdapter()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_unlink_with_this_device -> {
                DialogHelper.makeNegativePhrasedYesNoDialogBuilder(parentActivity, "OmniTrack", parentActivity.getString(R.string.msg_profile_unlink_account_confirm), R.string.msg_logout, onYes = {
                    authManager.signOut()
                    OTApp.instance.unlinkUser()
                    if (parentActivity is OTActivity) {
                        parentActivity.goSignIn()
                    }
                }).show()
                return true
            }
            else -> {
                return false
            }
        }
    }

    fun onCreate() {
        subscriptions.add(
                authManager.userNameObservable.subscribe { (name) ->
                    nameView.text = name
                }
        )

        subscriptions.add(
                authManager.userImageUrlObservable.subscribe { uri ->
                    Glide.with(parentActivity).load(uri).into(photoView)
                }
        )
    }

    fun onDestroy() {
        subscriptions.clear()
    }

    inner class SidebarMenuAdapter : RecyclerViewMenuAdapter() {

        private val menus = arrayListOf(
                RecyclerViewMenuAdapter.MenuItem(R.drawable.settings_dark, parentActivity.getString(R.string.msg_settings), null, {
                    val intent = Intent(parentActivity, SettingsActivity::class.java)
                    parentActivity.startActivityForResult(intent, SettingsActivity.REQUEST_CODE)
                }, true),

                RecyclerViewMenuAdapter.MenuItem(R.drawable.help_dark, parentActivity.getString(R.string.msg_about), null, {
                    val intent = Intent(parentActivity, AboutActivity::class.java)
                    parentActivity.startActivity(intent)
                }, true),

                RecyclerViewMenuAdapter.MenuItem(R.drawable.icon_refresh, "Refresh", null, {
                    //OTApp.instance.syncManager.performSynchronizationOf(ESyncDataType.ITEM)
                    syncManager.get().queueFullSync()
                    syncManager.get().reserveSyncServiceNow()
                }, true)
        )

        init {
        }

        override fun getLayout(viewType: Int): Int {
            return R.layout.simple_menu_element_with_icon_title_description_small
        }

        override fun getMenuItemAt(index: Int): MenuItem {
            return menus[index]
        }

        override fun getItemCount(): Int {
            return menus.size
        }


    }
}